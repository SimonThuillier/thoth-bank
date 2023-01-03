package com.bts.thoth.bank.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.kafka.reactor.consumer.ResilientKafkaConsumer;
import io.github.cdimascio.dotenv.Dotenv;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.HashMap;
import java.util.Map;

public class BankAsynchronousProjector {

    private static final Dotenv dotenv = Dotenv.load();
    private static final TimeBasedGenerator UUIDgenerator = Generators.timeBasedGenerator();
    private static PgPool pgPool;
    private static final Vertx vertx = Vertx.vertx();
    private static PgAsyncPool pgProjectionPool;
    private static WithdrawByMonthProjection withdrawByMonthProjection;

    private static ReceiverOptions<String, String> receiverOptions;

    public BankAsynchronousProjector(){

        Logger.getRootLogger().setLevel(Level.INFO);

        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(dotenv.get("PPG_HOST"))
                .setPort(Integer.parseInt(dotenv.get("PPG_PORT")))
                .setUser(dotenv.get("PPG_USER"))
                .setPassword(dotenv.get("PPG_PWD"))
                .setDatabase(dotenv.get("PPG_DB"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        pgPool = PgPool.pool(vertx, options, poolOptions);
        pgProjectionPool = PgAsyncPool.create(pgPool, jooqConfig);

        withdrawByMonthProjection = new AsyncWithdrawByMonthProjection(pgProjectionPool);
        withdrawByMonthProjection.init().subscribe();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, dotenv.get("KAFKA_HOST"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        receiverOptions = ReceiverOptions.<String, String>create(props);
    }

    public void run(){

        ResilientKafkaConsumer<String, String> resilientKafkaConsumer = ResilientKafkaConsumer.createFromFlow(
                "ThothBankConsumer-" + UUIDgenerator.generate(),
                ResilientKafkaConsumer.Config.create(
                        java.util.List.of(dotenv.get("KAFKA_TOPIC")),
                        "ThothBankConsumer-lol10",
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
                                Either<String, EventEnvelope<BankEvent, Tuple0, Tuple0>> parsedEvent = BankEventEnvelopeParser.parseEnvelope(json);
                                return parsedEvent;
                            })
                            .log()
                            .filter(Either::isRight)
                            .map(Either::get)
                            .log()
                            .flatMap(parsedEvent -> withdrawByMonthProjection.storeProjection(transaction, List.of(parsedEvent)))
                            )
                            .subscribe();
                });
    }
}
