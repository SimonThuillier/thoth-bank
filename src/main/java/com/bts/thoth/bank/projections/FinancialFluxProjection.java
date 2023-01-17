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

import java.math.BigDecimal;
import java.util.Objects;

import static io.vavr.API.*;
import static io.vavr.PartialFunction.unlift;
import static org.jooq.impl.DSL.val;

/**
 * this class defines the projection of financial flux
 * each new month of activity it stores the whole bank output and input flux and the delta
 */
public class FinancialFluxProjection implements ReactorProjection<PgAsyncTransaction, AccountEvent, Tuple0, Tuple0> {

    private final PgAsyncPool pgAsyncPool;

    protected String targetTable;

    public FinancialFluxProjection(PgAsyncPool pgAsyncPool) {
        this.targetTable = "financial_flux";
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
                                    if(t._2 instanceof AccountEvent.MoneyDeposited){
                                        return dsl.query("""
                                                insert into <targetTable> (year, month, inputs, outputs, delta, txcount) values ({0}, {1}, <amount>, 0, <amount>, 1)
                                                on conflict (year, month)
                                                do update set 
                                                inputs = <targetTable>.inputs + EXCLUDED.inputs, 
                                                delta = <targetTable>.delta + EXCLUDED.inputs, 
                                                txcount=<targetTable>.txcount + 1;"""
                                                        .replaceAll("<targetTable>", targetTable)
                                                        .replaceAll("<amount>", ((AccountEvent.MoneyDeposited) t._2).amount().toString()), // workaround to solve an odd bug of parameter considered null in this case,
                                                val(t._1.emissionDate.getYear()),
                                                val(t._1.emissionDate.getMonth().name()));
                                    }
                                    else if(t._2 instanceof AccountEvent.MoneyWithdrawn){
                                        return dsl.query("""
                                                insert into <targetTable> (year, month, inputs, outputs, delta, txcount) values ({0}, {1}, 0, <amount>, -<amount>, 1)
                                                on conflict (year, month)
                                                do update set 
                                                outputs = <targetTable>.outputs + EXCLUDED.outputs, 
                                                delta = <targetTable>.delta - EXCLUDED.outputs, 
                                                txcount=<targetTable>.txcount + 1;"""
                                                        .replaceAll("<targetTable>", targetTable)
                                                        .replaceAll("<amount>", ((AccountEvent.MoneyWithdrawn) t._2).amount().toString()), // workaround to solve an odd bug of parameter considered null in this case,
                                                val(t._1.emissionDate.getYear()),
                                                val(t._1.emissionDate.getMonth().name()));
                                    }
                                    return null;
                                })
                                .filter(Objects::nonNull)
                )
                .thenReturn(Tuple.empty());
    }

    public Mono<Integer> init() {
        return this.pgAsyncPool.executeMono(dsl -> dsl.query("""
                 CREATE TABLE IF NOT EXISTS <targetTable>(
                    year smallint,
                    month text,
                    inputs money,
                    outputs money,
                    delta money,
                    txcount integer,
                    created_at timestamp,
                    updated_at timestamp,
                    PRIMARY KEY (year, month)
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
