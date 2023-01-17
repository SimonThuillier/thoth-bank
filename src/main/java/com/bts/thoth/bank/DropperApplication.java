package com.bts.thoth.bank;

import com.bts.thoth.bank.config.EventStoreDataSourceConfig;
import com.bts.thoth.bank.config.KafkaConfig;
import com.bts.thoth.bank.config.ProjectionDataSourceConfig;
import com.bts.thoth.bank.projections.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.CommonClientConfigs;
import fr.maif.jooq.reactor.PgAsyncPool;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class DropperApplication {
    private static final Vertx vertx = Vertx.vertx();

    private static ApplicationContext context;

    /**
     * application to drop projection, event state tables, and kafka topics
     * its main goal is to reinitialize all the data part on the environment to start anew
     * @param args
     */
    public static void main(String[] args){

        context = new AnnotationConfigApplicationContext(
                "com.bts.thoth.bank", "com.bts.thoth.bank.config");

        dropProjections();
        dropKafkaEvents();
        dropEventSources();
    }

    /**
     * drop projection tables
     */
    private static void dropProjections(){

        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        ProjectionDataSourceConfig projectionDataSourceConfig = context.getBean(ProjectionDataSourceConfig.class);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(projectionDataSourceConfig.getHost())
                .setPort(projectionDataSourceConfig.getPort())
                .setUser(projectionDataSourceConfig.getUser())
                .setPassword(projectionDataSourceConfig.getPassword())
                .setDatabase(projectionDataSourceConfig.getDatabase());
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        PgPool pgProjectionBasePool = PgPool.pool(vertx, options, poolOptions);
        PgAsyncPool pgAsyncProjectionBasePool = PgAsyncPool.create(pgProjectionBasePool, jooqConfig);

        (new AccountProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new AsyncAccountProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new BalanceHistoryProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new AsyncBalanceHistoryProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new FinancialFluxProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new AsyncFinancialFluxProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new WithdrawByMonthProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new AsyncWithdrawByMonthProjection(pgAsyncProjectionBasePool)).drop().subscribe();
    }

    /**
     * drop all events in bank topics
     */
    private static void dropKafkaEvents(){

        KafkaConfig kafkaConfig = context.getBean(KafkaConfig.class);

        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getHost());

        AdminClient kafkaAdminClient = AdminClient.create(props);

        kafkaAdminClient.deleteTopics(Arrays.asList(kafkaConfig.getTopic()));
        kafkaAdminClient.close();

    }

    /**
     * drop events journal : since in event sourcing those are the primary root
     * of all information this method has irrevocable effect.
     */
    private static void dropEventSources(){

        EventStoreDataSourceConfig eventStoreDataSourceConfig = context.getBean(EventStoreDataSourceConfig.class);

        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(eventStoreDataSourceConfig.getHost())
                .setPort(eventStoreDataSourceConfig.getPort())
                .setUser(eventStoreDataSourceConfig.getUser())
                .setPassword(eventStoreDataSourceConfig.getPassword())
                .setDatabase(eventStoreDataSourceConfig.getDatabase());
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        PgPool pgEventBasePool = PgPool.pool(vertx, options, poolOptions);
        PgAsyncPool pgAsyncEventBasePool = PgAsyncPool.create(pgEventBasePool, jooqConfig);

        Bank.drop(pgAsyncEventBasePool).subscribe();
    }




}
