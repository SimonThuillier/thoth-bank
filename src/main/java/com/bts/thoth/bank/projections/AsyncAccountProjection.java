package com.bts.thoth.bank.projections;

import fr.maif.jooq.reactor.PgAsyncPool;

public class AsyncAccountProjection extends AccountProjection {
    public AsyncAccountProjection(PgAsyncPool pgAsyncPool) {
        super(pgAsyncPool);
        targetTable = "async_account";
    }
}
