package com.bts.thoth.bank.repository;

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
public class AccountRepository {

    private final Vertx vertx;
    private final PgAsyncPool pgAsyncPool;

    public AccountRepository(ApplicationContext applicationContext) {

        this.vertx = Vertx.vertx();

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
    }

    public Mono<String> getById(String accountId) {
        return pgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select id, balance::DECIMAL, status, creation_date, closing_date from account where id = {0}
                            """, val(accountId)
                )
        ).map(r -> {
            if (r.size() == 0) {
                return "No account of id " + accountId + " found";
            }

            if(r.get(0).get("status", String.class).equals("OPEN")) {
                return "Account "
                        + r.get(0).get("id", String.class)
                        + " of current balance=" + r.get(0).get("balance", String.class)
                        + " " + r.get(0).get("status", String.class)
                        + " since " + r.get(0).get("creation_date", Date.class);
            }

            return "Account "
                    + r.get(0).get("id", String.class)
                    + " of residual balance " + r.get(0).get("balance", BigDecimal.class)
                    + " open on " + r.get(0).get("creation_date", Date.class)
                    + " and " + r.get(0).get("status", String.class)
                    + " on " + r.get(0).get("closing_date", Date.class);
        });
    }

    public Mono<String> findAccountIds(String status, String sense, Integer count) {
        return pgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select id from account where status={0} order by creation_date <sense> limit {2}
                            """.replaceAll("<sense>", sense), val(status) , val(sense), val(count)
                )
        ).map(r -> {
            if (r.size() == 0) {
                return "No account found";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.size(); i++) {
                sb.append(r.get(i).get("id", String.class)).append("\r\n");
            }
            return sb.toString();
        });
    }


}
