package com.bts.thoth.bank;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;

import com.bts.thoth.bank.account.*;
import com.bts.thoth.bank.config.EventStoreDataSourceConfig;
import com.bts.thoth.bank.config.KafkaConfig;
import com.bts.thoth.bank.config.ProjectionDataSourceConfig;
import com.bts.thoth.bank.projections.AccountProjection;
import com.bts.thoth.bank.projections.BalanceHistoryProjection;
import com.bts.thoth.bank.projections.FinancialFluxProjection;
import com.bts.thoth.bank.projections.WithdrawByMonthProjection;
import fr.maif.eventsourcing.*;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import static io.vavr.API.List;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderOptions;

import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.jooq.reactor.PgAsyncTransaction;
import fr.maif.kafka.JsonFormatSerDer;
import fr.maif.reactor.kafka.KafkaSettings;

@Component
public class Bank implements Closeable {

    private static final TimeBasedGenerator UUIDgenerator = Generators.timeBasedGenerator();
    private static final String accountTable = """
            CREATE TABLE IF NOT EXISTS ACCOUNTS (
                id varchar(100) PRIMARY KEY,
                balance money NOT NULL
            );""";

    private static final String dropAccountTable = "DROP TABLE IF EXISTS ACCOUNTS;";
    private static final String bankJournalTable = """

            CREATE TABLE IF NOT EXISTS bank_journal (
              id UUID primary key,
              entity_id varchar(100) not null,
              sequence_num bigint not null,
              event_type varchar(100) not null,
              version int not null,
              transaction_id varchar(100) not null,
              event jsonb not null,
              metadata jsonb,
              context jsonb,
              total_message_in_transaction int default 1,
              num_message_in_transaction int default 1,
              emission_date timestamp not null default now(),
              user_id varchar(100),
              system_id varchar(100),
              published boolean default false,
              UNIQUE (entity_id, sequence_num)
            );""";

    private static final String dropBankJournalTable = "DROP TABLE IF EXISTS bank_journal;";
    private static final String SEQUENCE = """
                    CREATE SEQUENCE if not exists bank_sequence_num;
            """;

    private static final String dropSequence = "DROP SEQUENCE IF EXISTS bank_sequence_num;";

    KafkaConfig kafkaConfig;
    EventStoreDataSourceConfig eventStoreDataSourceConfig;
    ProjectionDataSourceConfig projectionDataSourceConfig;

    private final PgAsyncPool projectionPgAsyncPool;
    private PgPool pgPool;
    private final Vertx vertx;
    private final PgAsyncPool pgAsyncPool; // event store projection
    private PgPool projectionPgPool;
    private final ReactorEventProcessor<String, Account, AccountCommand, AccountEvent, PgAsyncTransaction, Tuple0, Tuple0, Tuple0> eventProcessor;
    private final AccountProjection accountProjection;
    private final BalanceHistoryProjection balanceHistoryProjection;
    private final FinancialFluxProjection financialFluxProjection;
    private final WithdrawByMonthProjection withdrawByMonthProjection;

    public Bank(
            ApplicationContext applicationContext,
            AccountCommandHandler commandHandler,
            AccountEventHandler eventHandler
    ) {
        kafkaConfig = applicationContext.getBean(KafkaConfig.class);
        eventStoreDataSourceConfig = applicationContext.getBean(EventStoreDataSourceConfig.class);
        projectionDataSourceConfig = applicationContext.getBean(ProjectionDataSourceConfig.class);

        this.vertx = Vertx.vertx();
        this.pgAsyncPool = pgAsyncPool(vertx);
        this.projectionPgAsyncPool = projectionPgAsyncPool(vertx);
        this.withdrawByMonthProjection = new WithdrawByMonthProjection(projectionPgAsyncPool);
        this.accountProjection = new AccountProjection(projectionPgAsyncPool);
        this.balanceHistoryProjection = new BalanceHistoryProjection(projectionPgAsyncPool);
        this.financialFluxProjection = new FinancialFluxProjection(projectionPgAsyncPool);

        this.eventProcessor = ReactiveEventProcessor
                .withPgAsyncPool(pgAsyncPool)
                .withTables(tableNames())
                .withTransactionManager()
                .withEventFormater(AccountEventFormat.accountEventFormat.jacksonEventFormat())
                .withNoMetaFormater()
                .withNoContextFormater()
                .withKafkaSettings(
                        kafkaConfig.getTopic(),
                        senderOptions(settings()))
                .withEventHandler(eventHandler)
                .withDefaultAggregateStore()
                .withCommandHandler(commandHandler)
                .withProjections(
                        this.accountProjection
                        //this.withdrawByMonthProjection,
                        //this.balanceHistoryProjection,
                        //this.financialFluxProjection
                )
                .build();
    }

    public Mono<Tuple0> init() {
        LoggerFactory.getLogger("Bank").info("Initializing database");
        return Flux.fromIterable(List(bankJournalTable, SEQUENCE))
                .concatMap(script -> pgAsyncPool.executeMono(d -> d.query(script)))
                .collectList()
                .doOnSuccess(__ -> LoggerFactory.getLogger("Bank").info("Database initialized"))
                .doOnError(e -> {
                    LoggerFactory.getLogger("Bank").error("Database initialization failed");
                    e.printStackTrace();
                })
                .flatMap(__ -> accountProjection.init())
//                .flatMap(__ -> balanceHistoryProjection.init())
//                .flatMap(__ -> financialFluxProjection.init())
//                .flatMap(__ -> withdrawByMonthProjection.init())
                .thenReturn(Tuple.empty());
    }

    public static Mono<Tuple0> drop(PgAsyncPool pgAsyncPool) {
        return Flux.fromIterable(List(dropAccountTable, dropBankJournalTable, dropSequence))
                .concatMap(script -> pgAsyncPool.executeMono(d -> d.query(script)))
                .collectList()
                .thenReturn(Tuple.empty());
    }

    @Override
    public void close() throws IOException {
        this.pgPool.close();
        this.projectionPgPool.close();
        this.vertx.close();
        // this.projectionVertx.close();
    }

    private PgAsyncPool pgAsyncPool(Vertx vertx) {
        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(eventStoreDataSourceConfig.getHost())
                .setPort(eventStoreDataSourceConfig.getPort())
                .setUser(eventStoreDataSourceConfig.getUser())
                .setPassword(eventStoreDataSourceConfig.getPassword())
                .setDatabase(eventStoreDataSourceConfig.getDatabase());
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        pgPool = PgPool.pool(vertx, options, poolOptions);

        return PgAsyncPool.create(pgPool, jooqConfig);
    }

    private PgAsyncPool projectionPgAsyncPool(Vertx vertx) {
        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(projectionDataSourceConfig.getHost())
                .setPort(projectionDataSourceConfig.getPort())
                .setUser(projectionDataSourceConfig.getUser())
                .setPassword(projectionDataSourceConfig.getPassword())
                .setDatabase(projectionDataSourceConfig.getDatabase());
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        projectionPgPool = PgPool.pool(vertx, options, poolOptions);

        return PgAsyncPool.create(projectionPgPool, jooqConfig);
    }

    private KafkaSettings settings() {
        return KafkaSettings.newBuilder(kafkaConfig.getHost()).build();
    }

    private SenderOptions<String, EventEnvelope<AccountEvent, Tuple0, Tuple0>> senderOptions(KafkaSettings kafkaSettings) {
        return kafkaSettings.producerSettings(JsonFormatSerDer.of(AccountEventFormat.accountEventFormat));
    }

    private TableNames tableNames() {
        return new TableNames("bank_journal", "bank_sequence_num");
    }


    public Mono<Either<String, Account>> createAccount(
            BigDecimal amount) {
        Lazy<String> lazyId = Lazy.of(() -> UUIDgenerator.generate().toString());
        return eventProcessor.processCommand(new AccountCommand.OpenAccount(lazyId, amount))
                .map(res -> res.flatMap(processingResult -> processingResult.currentState.toEither("Current state is missing")));
    }

    public Mono<Either<String, Account>> withdraw(
            String account, BigDecimal amount) {
        return eventProcessor.processCommand(new AccountCommand.Withdraw(account, amount))
                .map(res ->
                        res.flatMap(processingResult -> processingResult.currentState.toEither("Current state is missing")));
    }

    public Mono<Either<String, Account>> deposit(
            String account, BigDecimal amount) {
        return eventProcessor.processCommand(new AccountCommand.Deposit(account, amount))
                .map(res ->
                        res.flatMap(processingResult -> processingResult.currentState.toEither("Current state is missing")));
    }

    public Mono<Either<String, ProcessingSuccess<Account, AccountEvent, Tuple0, Tuple0, Tuple0>>> close(
            String account) {
        return eventProcessor.processCommand(new AccountCommand.CloseAccount(account));
    }

    public Mono<Option<Account>> findAccountById(String id) {
        return eventProcessor.getAggregate(id);
    }

    public Mono<BigDecimal> meanWithdrawByClientAndMonth(String clientId, Integer year, String month) {
        //return withdrawByMonthProjection.meanWithdrawByClientAndMonth(clientId, year, month);
        return Mono.empty();
    }

    public Mono<BigDecimal> meanWithdrawByClient(String clientId) {
        //return withdrawByMonthProjection.meanWithdrawByClient(clientId);
        return Mono.empty();
    }
}