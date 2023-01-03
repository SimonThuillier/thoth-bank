package com.bts.thoth.bank;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;

import com.bts.thoth.bank.core.*;
import fr.maif.eventsourcing.*;
import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import static io.vavr.API.List;
import static io.vavr.API.println;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderOptions;

import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.jooq.reactor.PgAsyncTransaction;
import fr.maif.kafka.JsonFormatSerDer;
import fr.maif.reactor.kafka.KafkaSettings;

public class Bank implements Closeable {

    private static final Dotenv dotenv = Dotenv.load();
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

    private final PgAsyncPool projectionPgAsyncPool;
    private PgPool pgPool;
    private final Vertx vertx;
    private final PgAsyncPool pgAsyncPool; // event store projection
    private PgPool projectionPgPool;
    private final ReactorEventProcessor<String, Account, BankCommand, BankEvent, PgAsyncTransaction, Tuple0, Tuple0, Tuple0> eventProcessor;
    private final WithdrawByMonthProjection withdrawByMonthProjection;

    public Bank(BankCommandHandler commandHandler, BankEventHandler eventHandler) {
        this.vertx = Vertx.vertx();
        this.pgAsyncPool = pgAsyncPool(vertx);
        this.projectionPgAsyncPool = projectionPgAsyncPool(vertx);
        this.withdrawByMonthProjection = new WithdrawByMonthProjection(projectionPgAsyncPool);

        this.eventProcessor = ReactiveEventProcessor
                .withPgAsyncPool(pgAsyncPool)
                .withTables(tableNames())
                .withTransactionManager()
                .withEventFormater(BankEventFormat.bankEventFormat.jacksonEventFormat())
                .withNoMetaFormater()
                .withNoContextFormater()
                .withKafkaSettings(dotenv.get("KAFKA_TOPIC"), senderOptions(settings()))
                .withEventHandler(eventHandler)
                .withDefaultAggregateStore()
                .withCommandHandler(commandHandler)
                .withProjections(this.withdrawByMonthProjection)
                .build();
    }

    public Mono<Tuple0> init() {
        println("Initializing database");
        return Flux.fromIterable(List(accountTable, bankJournalTable, SEQUENCE))
                .concatMap(script -> pgAsyncPool.executeMono(d -> d.query(script)))
                .collectList()
                .doOnSuccess(__ -> println("Database initialized"))
                .doOnError(e -> {
                    println("Database initialization failed");
                    e.printStackTrace();
                })
                .flatMap(__ -> withdrawByMonthProjection.init())
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
                .setHost(dotenv.get("EPG_HOST"))
                .setPort(Integer.parseInt(dotenv.get("EPG_PORT")))
                .setUser(dotenv.get("EPG_USER"))
                .setPassword(dotenv.get("EPG_PWD"))
                .setDatabase(dotenv.get("EPG_DB"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        pgPool = PgPool.pool(vertx, options, poolOptions);

        return PgAsyncPool.create(pgPool, jooqConfig);
    }

    private PgAsyncPool projectionPgAsyncPool(Vertx vertx) {
        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(dotenv.get("PPG_HOST"))
                .setPort(Integer.parseInt(dotenv.get("PPG_PORT")))
                .setUser(dotenv.get("PPG_USER"))
                .setPassword(dotenv.get("PPG_PWD"))
                .setDatabase(dotenv.get("PPG_DB"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        projectionPgPool = PgPool.pool(vertx, options, poolOptions);

        return PgAsyncPool.create(projectionPgPool, jooqConfig);
    }

    private KafkaSettings settings() {
        return KafkaSettings.newBuilder(dotenv.get("KAFKA_HOST")).build();
    }

    private SenderOptions<String, EventEnvelope<BankEvent, Tuple0, Tuple0>> senderOptions(KafkaSettings kafkaSettings) {
        return kafkaSettings.producerSettings(JsonFormatSerDer.of(BankEventFormat.bankEventFormat));
    }

    private TableNames tableNames() {
        return new TableNames("bank_journal", "bank_sequence_num");
    }


    public Mono<Either<String, Account>> createAccount(
            BigDecimal amount) {
        Lazy<String> lazyId = Lazy.of(() -> UUIDgenerator.generate().toString());
        return eventProcessor.processCommand(new BankCommand.OpenAccount(lazyId, amount))
                .map(res -> res.flatMap(processingResult -> processingResult.currentState.toEither("Current state is missing")));
    }

    public Mono<Either<String, Account>> withdraw(
            String account, BigDecimal amount) {
        return eventProcessor.processCommand(new BankCommand.Withdraw(account, amount))
                .map(res -> res.flatMap(processingResult -> processingResult.currentState.toEither("Current state is missing")));
    }

    public Mono<Either<String, Account>> deposit(
            String account, BigDecimal amount) {
        return eventProcessor.processCommand(new BankCommand.Deposit(account, amount))
                .map(res -> res.flatMap(processingResult -> processingResult.currentState.toEither("Current state is missing")));
    }

    public Mono<Either<String, ProcessingSuccess<Account, BankEvent, Tuple0, Tuple0, Tuple0>>> close(
            String account) {
        return eventProcessor.processCommand(new BankCommand.CloseAccount(account));
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