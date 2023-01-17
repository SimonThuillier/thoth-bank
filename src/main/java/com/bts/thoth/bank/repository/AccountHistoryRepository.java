package com.bts.thoth.bank.repository;

import com.bts.thoth.bank.config.EventStoreDataSourceConfig;
import com.bts.thoth.bank.config.ProjectionDataSourceConfig;
import fr.maif.jooq.QueryResult;
import fr.maif.jooq.reactor.PgAsyncPool;
import io.vavr.collection.List;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;
import java.util.NoSuchElementException;

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

    /**
     * get history of accounts joining data between event store and projection store
     * @param accountId
     * @param limit
     * @return
     */
    public Mono<String> getHistoryByAccountId2(String accountId, Integer limit) {

        Mono<List<QueryResult>> esRecordMono = esPgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select id as event_id, event_type, emission_date,
                               CASE
                                   WHEN event_type = 'MoneyDeposited'  THEN event::json->>'amount'
                                   WHEN event_type = 'MoneyWithdrawn'  THEN event::json->>'amount'
                              ELSE 'NA'
                                   END as variation
                               from bank_journal where entity_id={0} order by sequence_num desc limit {1};
                            """, val(accountId), val(limit)
                )
        );
        Mono<List<QueryResult>> projectionRecordMono = pgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select event_id, balance::DECIMAL from balance_history where account_id={0} order by sequence_num desc limit {1};
                            """, val(accountId), val(limit)
                )
        );

        return Mono.zip(esRecordMono, projectionRecordMono)
                .map(tuple -> {
                    List<QueryResult> esRecords = tuple.getT1();
                    List<QueryResult> balanceRecords = tuple.getT2();
                    if (esRecords.size() == 0) {
                        return "No account of id " + accountId + " found";
                    }
                    String eventType;
                    StringBuilder sb = new StringBuilder();
                    for (QueryResult esRecord: esRecords) {
                        eventType = esRecord.get("event_type", String.class);

                        QueryResult balanceRecord;
                        try{
                            balanceRecord = balanceRecords
                                    .filter(b -> b.get("event_id", String.class).equals(esRecord.get("event_id", String.class)))
                                    .peek();
                        } catch (NoSuchElementException e) {
                            balanceRecord = null;
                        }

                        String strBuffer = "";


                        switch (eventType) {
                            case "AccountOpened":
                                strBuffer = "Opening on " + esRecord.get("emission_date", Date.class);
                                break;
                            case "MoneyDeposited":
                                strBuffer = "Deposit on " + esRecord.get("emission_date", Date.class) + ": +" + esRecord.get("variation", BigDecimal.class);
                                break;
                            case "MoneyWithdrawn":
                                strBuffer = "Withdraw on " + esRecord.get("emission_date", Date.class) + ": -" + esRecord.get("variation", BigDecimal.class);
                                break;
                            case "AccountClosed":
                                strBuffer = "Closing on " + esRecord.get("emission_date", Date.class);
                                break;
                            default:
                                break;
                        }
                        strBuffer += balanceRecord != null ? ", balance: " + balanceRecord.get("balance", BigDecimal.class) + "\r\n" : "\r\n";
                        sb.append(strBuffer);
                    }
                    return sb.toString();
                });
    }
}
