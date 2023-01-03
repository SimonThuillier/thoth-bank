package com.bts.thoth.bank;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.CommonClientConfigs;
import com.bts.thoth.bank.core.AsyncWithdrawByMonthProjection;
import com.bts.thoth.bank.core.WithdrawByMonthProjection;
import fr.maif.jooq.reactor.PgAsyncPool;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class DropperApplication {

    private static final Dotenv dotenv = Dotenv.load();

    private static final Vertx vertx = Vertx.vertx();

    /**
     * application to drop projection, event state tables, and kafka topics
     * its main goal is to reinitialize all the data part on the environment to start anew
     * @param args
     */
    public static void main(String[] args){
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

        PgConnectOptions options = new PgConnectOptions()
                .setHost(dotenv.get("PPG_HOST"))
                .setPort(Integer.parseInt(dotenv.get("PPG_PORT")))
                .setUser(dotenv.get("PPG_USER"))
                .setPassword(dotenv.get("PPG_PWD"))
                .setDatabase(dotenv.get("PPG_DB"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        PgPool pgProjectionBasePool = PgPool.pool(vertx, options, poolOptions);
        PgAsyncPool pgAsyncProjectionBasePool = PgAsyncPool.create(pgProjectionBasePool, jooqConfig);

        (new AsyncWithdrawByMonthProjection(pgAsyncProjectionBasePool)).drop().subscribe();
        (new WithdrawByMonthProjection(pgAsyncProjectionBasePool)).drop().subscribe();
    }

    /**
     * drop all events in bank topics
     */
    private static void dropKafkaEvents(){

        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, dotenv.get("KAFKA_HOST"));

        AdminClient kafkaAdminClient = AdminClient.create(props);

        kafkaAdminClient.deleteTopics(Arrays.asList(dotenv.get("KAFKA_TOPIC")));
        kafkaAdminClient.close();

    }

    /**
     * drop events journal : since in event sourcing those are the primary root
     * of all information this method has irrevocable effect.
     */
    private static void dropEventSources(){

        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(dotenv.get("EPG_HOST"))
                .setPort(Integer.parseInt(dotenv.get("EPG_PORT")))
                .setUser(dotenv.get("EPG_USER"))
                .setPassword(dotenv.get("EPG_PWD"))
                .setDatabase(dotenv.get("EPG_DB"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
        PgPool pgEventBasePool = PgPool.pool(vertx, options, poolOptions);
        PgAsyncPool pgAsyncEventBasePool = PgAsyncPool.create(pgEventBasePool, jooqConfig);

        Bank.drop(pgAsyncEventBasePool).subscribe();
    }




}
