package com.bts.thoth.bank;

import com.bts.thoth.bank.account.AccountEvent;
import com.bts.thoth.bank.account.AccountEventEnvelopeParser;
import com.bts.thoth.bank.config.EventStoreDataSourceConfig;
import com.bts.thoth.bank.config.KafkaConfig;
import com.bts.thoth.bank.config.ProjectionDataSourceConfig;
import com.bts.thoth.bank.projections.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.kafka.reactor.consumer.ResilientKafkaConsumer;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.HashMap;
import java.util.Map;

@Component
public class BankAsynchronousProjector {

    private static final TimeBasedGenerator UUIDgenerator = Generators.timeBasedGenerator();
    private static PgPool pgPool;
    private static final Vertx vertx = Vertx.vertx();
    private static PgAsyncPool pgProjectionPool;

    private final AsyncAccountProjection accountProjection;
    private final AsyncBalanceHistoryProjection balanceHistoryProjection;
    private final AsyncFinancialFluxProjection financialFluxProjection;
    private static AsyncWithdrawByMonthProjection withdrawByMonthProjection;

    private static ReceiverOptions<String, String> receiverOptions;

    private KafkaConfig kafkaConfig;

    public BankAsynchronousProjector(ApplicationContext applicationContext){

        kafkaConfig = applicationContext.getBean(KafkaConfig.class);
        ProjectionDataSourceConfig projectionDataSourceConfig = applicationContext.getBean(ProjectionDataSourceConfig.class);

        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(projectionDataSourceConfig.getHost())
                .setPort(projectionDataSourceConfig.getPort())
                .setUser(projectionDataSourceConfig.getUser())
                .setPassword(projectionDataSourceConfig.getPassword())
                .setDatabase(projectionDataSourceConfig.getDatabase());
        PoolOptions poolOptions = new PoolOptions().setMaxSize(10);
        pgPool = PgPool.pool(vertx, options, poolOptions);
        pgProjectionPool = PgAsyncPool.create(pgPool, jooqConfig);

        accountProjection = new AsyncAccountProjection(pgProjectionPool);
        accountProjection.init().subscribe();
        balanceHistoryProjection = new AsyncBalanceHistoryProjection(pgProjectionPool);
        balanceHistoryProjection.init().subscribe();
        financialFluxProjection = new AsyncFinancialFluxProjection(pgProjectionPool);
        financialFluxProjection.init().subscribe();
        withdrawByMonthProjection = new AsyncWithdrawByMonthProjection(pgProjectionPool);
        withdrawByMonthProjection.init().subscribe();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getHost());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        receiverOptions = ReceiverOptions.<String, String>create(props);
    }

    public void run(){

        ResilientKafkaConsumer<String, String> resilientKafkaConsumer = ResilientKafkaConsumer.createFromFlow(
                "ThothBankConsumer-" + UUIDgenerator.generate(),
                ResilientKafkaConsumer.Config.create(
                        java.util.List.of(kafkaConfig.getTopic()),
                        "ThothBankConsumer-4",
                        receiverOptions
                ),
                this::kafkaEventPipeline
        );
    }

    private Flux<ReceiverRecord<String, String>> kafkaEventPipeline(Flux<ReceiverRecord<String, String>> kafkaFlux){

        ObjectMapper map = new ObjectMapper();

        return kafkaFlux
                .log()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(record -> {
                    pgProjectionPool.inTransactionMono(transaction -> Mono
                            .just(record)
                            .map(e -> {
                                try {
                                    return map.readTree(e.value());
                                } catch (JsonProcessingException ex) {
                                    throw Exceptions.propagate(ex);
                                }
                            })
                            .log()
                            .map(json -> {
                                Either<String, EventEnvelope<AccountEvent, Tuple0, Tuple0>> parsedEvent = AccountEventEnvelopeParser.parseEnvelope(json);
                                return parsedEvent;
                            })
                            .log()
                            .filter(Either::isRight)
                            .map(Either::get)
                            .log()
                            .publishOn(Schedulers.boundedElastic())
                            .doOnNext(parsedEvent -> {
                                accountProjection.storeProjection(transaction, List.of(parsedEvent)).subscribe();
                                balanceHistoryProjection.storeProjection(transaction, List.of(parsedEvent)).subscribe();
                                financialFluxProjection.storeProjection(transaction, List.of(parsedEvent)).subscribe();
                                withdrawByMonthProjection.storeProjection(transaction, List.of(parsedEvent)).subscribe();
                            })
                            )
                            .subscribe();
                });
    }
}
