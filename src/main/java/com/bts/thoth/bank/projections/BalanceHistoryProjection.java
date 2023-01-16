package com.bts.thoth.bank.projections;

import com.bts.thoth.bank.account.AccountEvent;
import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.eventsourcing.ReactorProjection;
import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.jooq.reactor.PgAsyncTransaction;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static io.vavr.API.Some;
import static io.vavr.API.Tuple;
import static io.vavr.PartialFunction.unlift;
import static org.jooq.impl.DSL.val;

/**
 * the projection of balance history a new insert is realized for each account at each event impacting balance
 */
public class BalanceHistoryProjection implements ReactorProjection<PgAsyncTransaction, AccountEvent, Tuple0, Tuple0> {

    private final PgAsyncPool pgAsyncPool;

    protected String targetTable;

    public BalanceHistoryProjection(PgAsyncPool pgAsyncPool) {
        this.targetTable = "balance_history";
        this.pgAsyncPool = pgAsyncPool;
    }

    @Override
    public Mono<Tuple0> storeProjection(PgAsyncTransaction connection, List<EventEnvelope<AccountEvent, Tuple0, Tuple0>> envelopes) {
        // warning : during execution connection injected by EventProcessorImpl is the one used by event store
        // projection should use its own connection for the most part of cases
        return this.pgAsyncPool.executeBatchMono(dsl ->
                envelopes
                        .collect(unlift(eventEnvelope -> Some(Tuple(eventEnvelope, eventEnvelope.event)) ))

                        .map(t -> {
                            if (t._2 instanceof AccountEvent.AccountOpened) {
                                return dsl.query("""
                                                insert into <targetTable> (event_id, account_id, sequence_num, balance, emission_date) values ({0}, {1}, <sequence_num>, 0, {3})
                                                on conflict DO NOTHING;"""
                                                .replaceAll("<targetTable>", targetTable)
                                                .replaceAll("<sequence_num>", t._1.sequenceNum.toString()),
                                        val(t._1.id),
                                        val(t._1.entityId),
                                        val(t._1.sequenceNum),
                                        val(t._1.emissionDate));
                            }
                            else if(t._2 instanceof AccountEvent.MoneyDeposited){
                                return dsl.query("""
                                                insert into <targetTable> (event_id, account_id, sequence_num, balance, emission_date) 
                                                select {0}, {1}, <sequence_num>,
                                                (select balance
                                                from balance_history
                                                where account_id={1}
                                                order by sequence_num desc limit 1) + <amount>::money
                                                , {3}
                                                on conflict DO NOTHING;"""
                                                .replaceAll("<targetTable>", targetTable)
                                                .replaceAll("<sequence_num>", t._1.sequenceNum.toString())
                                                .replaceAll("<amount>", ((AccountEvent.MoneyDeposited) t._2).amount().toString()),
                                        val(t._1.id),
                                        val(t._1.entityId),
                                        val(t._1.sequenceNum),
                                        val(t._1.emissionDate));
                            }
                            else if(t._2 instanceof AccountEvent.MoneyWithdrawn){
                                return dsl.query("""
                                                insert into <targetTable> (event_id, account_id, sequence_num, balance, emission_date) 
                                                select {0}, {1}, <sequence_num>,
                                                (select balance
                                                from balance_history
                                                where account_id={1}
                                                order by sequence_num desc limit 1) - <amount>::money
                                                , {3}
                                                on conflict DO NOTHING;"""
                                                .replaceAll("<targetTable>", targetTable)
                                                .replaceAll("<sequence_num>", t._1.sequenceNum.toString())
                                                .replaceAll("<amount>", ((AccountEvent.MoneyWithdrawn) t._2).amount().toString()),
                                        val(t._1.id),
                                        val(t._1.entityId),
                                        val(t._1.sequenceNum),
                                        val(t._1.emissionDate));
                            }
                            else if(t._2 instanceof AccountEvent.AccountClosed){
                                return dsl.query("""
                                                insert into <targetTable> (event_id, account_id, sequence_num, balance, emission_date) values ({0}, {1}, <sequence_num>, 0, {3})
                                                on conflict DO NOTHING;"""
                                                .replaceAll("<targetTable>", targetTable)
                                                .replaceAll("<sequence_num>", t._1.sequenceNum.toString()),
                                        val(t._1.id),
                                        val(t._1.entityId),
                                        val(t._1.sequenceNum),
                                        val(t._1.emissionDate));
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                )
                        .thenReturn(Tuple.empty());
    }

    public Mono<Integer> init() {
        return this.pgAsyncPool.executeMono(dsl -> dsl.query("""
                 CREATE TABLE IF NOT EXISTS <targetTable> (
                    event_id varchar(100) PRIMARY KEY,
                    account_id varchar(100),
                    sequence_num bigint NOT NULL,
                    balance money NOT NULL,
                    emission_date timestamp,
                    created_at timestamp,
                    updated_at timestamp
                    );"""
                        .replaceAll("<targetTable>", targetTable)))
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        CREATE OR REPLACE FUNCTION on_insert_f()
                        RETURNS TRIGGER
                        LANGUAGE PLPGSQL
                        AS
                        $$
                        BEGIN
                            NEW.created_at = now();
                            NEW.updated_at = NEW.created_at;
                            RETURN NEW;
                        END;
                        $$;"""))
                )
                .onErrorReturn(1)
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        CREATE TRIGGER on_insert BEFORE INSERT
                        ON <targetTable> FOR EACH ROW
                        EXECUTE FUNCTION on_insert_f();""".replaceAll("<targetTable>", targetTable)))
                )
                .onErrorReturn(1);
    }

    public Mono<Integer> drop(){
        return this.pgAsyncPool.executeMono(dsl -> dsl.query("""
                 DROP TABLE IF EXISTS <targetTable>;"""
                .replaceAll("<targetTable>", targetTable)))
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        DROP FUNCTION IF EXISTS on_insert_f;"""))
                        );
    }
}
