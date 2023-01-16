package com.bts.thoth.bank.repository;

import com.bts.thoth.bank.config.EventStoreDataSourceConfig;
import com.bts.thoth.bank.config.ProjectionDataSourceConfig;
import fr.maif.jooq.reactor.PgAsyncPool;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

import static org.jooq.impl.DSL.val;

@Component
public class AccountHistoryRepository {

    private final Vertx vertx;
    private final PgAsyncPool esPgAsyncPool;
    private final PgAsyncPool pgAsyncPool;

    public AccountHistoryRepository(ApplicationContext applicationContext) {

        this.vertx = Vertx.vertx();

        EventStoreDataSourceConfig esDataSourceConfig = applicationContext.getBean(EventStoreDataSourceConfig.class);
        ProjectionDataSourceConfig projectionDataSourceConfig = applicationContext.getBean(ProjectionDataSourceConfig.class);

        DefaultConfiguration jooqConfig = new DefaultConfiguration();
        jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

        PgConnectOptions options = new PgConnectOptions()
                .setHost(projectionDataSourceConfig.getHost())
                .setPort(projectionDataSourceConfig.getPort())
                .setUser(projectionDataSourceConfig.getUser())
                .setPassword(projectionDataSourceConfig.getPassword())
                .setDatabase(projectionDataSourceConfig.getDatabase());
        PoolOptions poolOptions = new PoolOptions().setMaxSize(50);

        this.pgAsyncPool = PgAsyncPool.create(PgPool.pool(vertx, options, poolOptions), jooqConfig);


        PgConnectOptions esOptions = new PgConnectOptions()
                .setHost(esDataSourceConfig.getHost())
                .setPort(esDataSourceConfig.getPort())
                .setUser(esDataSourceConfig.getUser())
                .setPassword(esDataSourceConfig.getPassword())
                .setDatabase(esDataSourceConfig.getDatabase());

        this.esPgAsyncPool = PgAsyncPool.create(PgPool.pool(vertx, esOptions, poolOptions), jooqConfig);
    }

    public Mono<String> getHistoryByAccountId(String accountId, Integer limit) {
        return esPgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select event_type, emission_date,
                               CASE
                                   WHEN event_type = 'MoneyDeposited'  THEN event::json->>'amount'
                                   WHEN event_type = 'MoneyWithdrawn'  THEN event::json->>'amount'
                              ELSE 'NA'
                                   END as variation
                               from bank_journal where entity_id={0} order by sequence_num desc limit {1};
                            """, val(accountId), val(limit)
                )
        ).map(r -> {
            if (r.size() == 0) {
                return "No account of id " + accountId + " found";
            }
            String eventType;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.size(); i++) {
                eventType = r.get(i).get("event_type", String.class);

                switch (eventType) {
                    case "AccountOpened":
                        sb.append("Opening on " + r.get(i).get("emission_date", Date.class) + "\r\n");
                        break;
                    case "MoneyDeposited":
                        sb.append("Deposit on " + r.get(i).get("emission_date", Date.class) + ": +" + r.get(i).get("variation", BigDecimal.class) + "\r\n");
                        break;
                    case "MoneyWithdrawn":
                        sb.append("Withdraw on " + r.get(i).get("emission_date", Date.class) + ": -" + r.get(i).get("variation", BigDecimal.class) + "\r\n");
                        break;
                    case "AccountClosed":
                        sb.append("Closing on " + r.get(i).get("emission_date", Date.class) + "\r\n");
                        break;
                    default:
                        break;
                }
            }
            return sb.toString();
        });
    }

    public Mono<String> getHistoryByAccountId2(String accountId, Integer limit) {
        return esPgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select event_type, emission_date,
                               CASE
                                   WHEN event_type = 'MoneyDeposited'  THEN event::json->>'amount'
                                   WHEN event_type = 'MoneyWithdrawn'  THEN event::json->>'amount'
                              ELSE 'NA'
                                   END as variation
                               from bank_journal where entity_id={0} order by sequence_num desc limit {1};
                            """, val(accountId), val(limit)
                )
        ).map(r -> {
            if (r.size() == 0) {
                return "No account of id " + accountId + " found";
            }
            String eventType;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.size(); i++) {
                eventType = r.get(i).get("event_type", String.class);

                switch (eventType) {
                    case "AccountOpened":
                        sb.append("Opening on " + r.get(i).get("emission_date", Date.class) + "\r\n");
                        break;
                    case "MoneyDeposited":
                        sb.append("Deposit on " + r.get(i).get("emission_date", Date.class) + ": +" + r.get(i).get("variation", BigDecimal.class) + "\r\n");
                        break;
                    case "MoneyWithdrawn":
                        sb.append("Withdraw on " + r.get(i).get("emission_date", Date.class) + ": -" + r.get(i).get("variation", BigDecimal.class) + "\r\n");
                        break;
                    case "AccountClosed":
                        sb.append("Closing on " + r.get(i).get("emission_date", Date.class) + "\r\n");
                        break;
                    default:
                        break;
                }
            }
            return sb.toString();
        });
    }
}
