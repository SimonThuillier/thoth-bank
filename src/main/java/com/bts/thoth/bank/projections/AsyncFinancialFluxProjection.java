package com.bts.thoth.bank.projections;

import fr.maif.jooq.reactor.PgAsyncPool;

public class AsyncFinancialFluxProjection extends FinancialFluxProjection {
    public AsyncFinancialFluxProjection(PgAsyncPool pgAsyncPool) {
        super(pgAsyncPool);
        targetTable = "async_financial_flux";
    }
}
