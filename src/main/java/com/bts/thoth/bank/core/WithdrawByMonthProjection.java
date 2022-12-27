package com.bts.thoth.bank.core;

import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.eventsourcing.ReactorProjection;
import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.jooq.reactor.PgAsyncTransaction;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static io.vavr.API.*;
import static io.vavr.PartialFunction.unlift;
import static org.jooq.impl.DSL.val;

/**
 * this class defines the synchronized projection of mean withdraw (insert of update of accounts with their current state)
 */
public class WithdrawByMonthProjection implements ReactorProjection<PgAsyncTransaction, BankEvent, Tuple0, Tuple0> {

    private final PgAsyncPool pgAsyncPool;

    protected String targetTable;

    public WithdrawByMonthProjection(PgAsyncPool pgAsyncPool) {
        this.targetTable = "withdraw_by_month";
        this.pgAsyncPool = pgAsyncPool;
    }

    @Override
    public Mono<Tuple0> storeProjection(PgAsyncTransaction connection, List<EventEnvelope<BankEvent, Tuple0, Tuple0>> envelopes) {
        // warning : during execution connection injected by EventProcessorImpl is the one used by event store
        // projection should use its own connection for the most part of cases
        return this.pgAsyncPool.executeBatchMono(dsl ->
                envelopes
                        // Keep only MoneyWithdrawn events
                        .collect(unlift(eventEnvelope ->
                                switch (eventEnvelope.event) {
                                    case BankEvent.MoneyWithdrawn e -> Some(Tuple(eventEnvelope, e));
                                    default -> None();
                                }
                        ))
                        // Store withdraw by month
                        .map(t -> dsl.query("""
                                            insert into <targetTable> (client_id, month, year, withdraw, count) values ({0}, {1}, {2}, {3}, 1)
                                            on conflict on constraint <targetTable>_UNIQUE
                                            do update set withdraw = <targetTable>.withdraw + EXCLUDED.withdraw, count=<targetTable>.count + 1;"""
                                        .replaceAll("<targetTable>", targetTable),
                                val(t._2.entityId()),
                                val(t._1.emissionDate.getMonth().name()),
                                val(t._1.emissionDate.getYear()),
                                val(t._2.amount())
                        ))
        ).thenReturn(Tuple.empty());
    }

    public Mono<BigDecimal> meanWithdrawByClientAndMonth(String clientId, Integer year, String month) {
        return pgAsyncPool.queryMono(dsl -> dsl.resultQuery(
                """
                        select round(withdraw / count::decimal, 2) \s
                        from <targetTable>\s
                        where  client_id = {0} and year = {1} and month = {2};"""
                        .replaceAll("<targetTable>", targetTable),
                val(clientId),
                val(year),
                val(month))
        ).map(r -> r.head().get(0, BigDecimal.class));
    }

    public Mono<BigDecimal> meanWithdrawByClient(String clientId) {
        return pgAsyncPool.queryMono(dsl -> dsl
                .resultQuery(
                        """
                            select round(sum(withdraw) / sum(count)::decimal, 2) as sum
                            from <targetTable>\s
                            where  client_id = {0}"""
                                .replaceAll("<targetTable>", targetTable), val(clientId)
                )
        ).map(r -> r.head().get("sum", BigDecimal.class));
    }

    public Mono<Integer> init() {
        return this.pgAsyncPool.executeMono(dsl -> dsl.query("""
                 CREATE TABLE IF NOT EXISTS <targetTable>(
                    client_id text,
                    month text,
                    year smallint,
                    withdraw numeric,
                    count integer
                 );"""
                        .replaceAll("<targetTable>", targetTable)))
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        CREATE UNIQUE INDEX IF NOT EXISTS <targetTable>_UNIQUE_IDX ON\s
                        <targetTable>(client_id, month, year);"""
                                .replaceAll("<targetTable>", targetTable)))
                )
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        ALTER TABLE <targetTable>\s
                        DROP CONSTRAINT IF EXISTS <targetTable>_UNIQUE;"""
                                .replaceAll("<targetTable>", targetTable)))
                )
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        ALTER TABLE <targetTable>\s
                        ADD CONSTRAINT <targetTable>_UNIQUE UNIQUE\s
                        USING INDEX <targetTable>_UNIQUE_IDX;"""
                                .replaceAll("<targetTable>", targetTable)))
                );
    }
}
