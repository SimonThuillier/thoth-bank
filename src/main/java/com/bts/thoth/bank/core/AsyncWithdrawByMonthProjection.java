package com.bts.thoth.bank.core;

import fr.maif.jooq.reactor.PgAsyncPool;

public class AsyncWithdrawByMonthProjection extends WithdrawByMonthProjection {
    public AsyncWithdrawByMonthProjection(PgAsyncPool pgAsyncPool) {
        super(pgAsyncPool);
        targetTable = "async_withdraw_by_month";
    }
}
