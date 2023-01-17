package com.bts.thoth.bank.projections;

import fr.maif.jooq.reactor.PgAsyncPool;

public class AsyncBalanceHistoryProjection extends BalanceHistoryProjection {
    public AsyncBalanceHistoryProjection(PgAsyncPool pgAsyncPool) {
        super(pgAsyncPool);
        targetTable = "async_balance_history";
    }
}
