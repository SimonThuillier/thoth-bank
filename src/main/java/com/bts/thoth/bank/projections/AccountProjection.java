package com.bts.thoth.bank.projections;

import com.bts.thoth.bank.account.AccountEvent;
import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.eventsourcing.ReactorProjection;
import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.jooq.reactor.PgAsyncTransaction;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import io.vavr.control.Option;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

import static io.vavr.API.*;
import static io.vavr.PartialFunction.unlift;
import static org.jooq.impl.DSL.val;

/**
 * the account current state projection
 */
public class AccountProjection implements ReactorProjection<PgAsyncTransaction, AccountEvent, Tuple0, Tuple0> {

    private final PgAsyncPool pgAsyncPool;

    protected String targetTable;

    public AccountProjection(PgAsyncPool pgAsyncPool) {
        this.targetTable = "account";
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
                                                insert into <targetTable> (id, balance, status, creation_date) values ({0}, 0, 'OPEN', {1})
                                                on conflict DO NOTHING;""".replaceAll("<targetTable>", targetTable),
                                        val(t._2.entityId()),
                                        val(t._1.emissionDate));
                            }
                            else if(t._2 instanceof AccountEvent.MoneyDeposited){
                                return dsl.query("""
                                                update <targetTable> set balance=balance + (<amount>::MONEY), last_deposit_date ={1} where id = {2};"""
                                                .replaceAll("<targetTable>", targetTable)
                                                .replaceAll("<amount>", ((AccountEvent.MoneyDeposited) t._2).amount().toString()), // workaround to solve an odd bug of parameter considered null in this case
                                        val(((AccountEvent.MoneyDeposited) t._2).amount().toString()),
                                        val(t._1.emissionDate),
                                        val(t._2.entityId()));
                            }
                            else if(t._2 instanceof AccountEvent.MoneyWithdrawn){
                                return dsl.query("""
                                                update <targetTable> set balance = balance - (<amount>::MONEY), last_withdraw_date ={1} where id = {2};"""
                                                .replaceAll("<targetTable>", targetTable)
                                                .replaceAll("<amount>", ((AccountEvent.MoneyWithdrawn) t._2).amount().toString()), // workaround to solve an odd bug of parameter considered null in this case,
                                        val(((AccountEvent.MoneyWithdrawn) t._2).amount()),
                                        val(t._1.emissionDate),
                                        val(t._2.entityId()));
                            }
                            else if(t._2 instanceof AccountEvent.AccountClosed){
                                return dsl.query("""
                                                update <targetTable> set status = 'CLOSED', closing_date ={1}  where id = {0};"""
                                                .replaceAll("<targetTable>", targetTable),
                                        val(t._2.entityId()),
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
                    id varchar(100) PRIMARY KEY,
                    balance money NOT NULL,
                    status varchar(20) NOT NULL,
                    creation_date timestamp NOT NULL,
                    last_deposit_date timestamp,
                    last_withdraw_date timestamp,
                    closing_date timestamp,
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
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        CREATE OR REPLACE FUNCTION on_update_f()
                        RETURNS TRIGGER
                        LANGUAGE PLPGSQL
                        AS
                        $$
                        BEGIN
                            NEW.updated_at = now();
                            RETURN NEW;
                        END;
                        $$;"""))
                )
                .onErrorReturn(1)
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        CREATE TRIGGER on_update BEFORE UPDATE
                            ON <targetTable> FOR EACH ROW
                        EXECUTE FUNCTION on_update_f();""".replaceAll("<targetTable>", targetTable)))
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
                        DROP FUNCTION IF EXISTS on_update_f;""")))
                .flatMap(__ ->
                        pgAsyncPool.executeMono(dsl -> dsl.query("""
                        DROP FUNCTION IF EXISTS on_insert_f;"""))
                        );
    }
}
