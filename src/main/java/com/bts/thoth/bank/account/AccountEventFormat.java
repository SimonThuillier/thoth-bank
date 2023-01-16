package com.bts.thoth.bank.account;

import com.bts.thoth.bank.account.AccountEvent.*;
import fr.maif.eventsourcing.Type;
import fr.maif.json.EventEnvelopeJsonFormat;
import fr.maif.json.JsonRead;
import fr.maif.json.JsonWrite;
import io.vavr.Tuple0;
import io.vavr.Tuple2;
import io.vavr.collection.List;

import static io.vavr.API.List;
import static io.vavr.API.Tuple;

public class AccountEventFormat implements EventEnvelopeJsonFormat<AccountEvent, Tuple0, Tuple0> {

    public static AccountEventFormat accountEventFormat = new AccountEventFormat();

    @Override
    public List<Tuple2<Type<? extends AccountEvent>, JsonRead<? extends AccountEvent>>> cases() {
        return List(
                Tuple(AccountEvent.MoneyWithdrawnV1, MoneyWithdrawn.format),
                Tuple(AccountEvent.AccountOpenedV1, AccountOpened.format),
                Tuple(AccountEvent.MoneyDepositedV1, MoneyDeposited.format),
                Tuple(AccountEvent.AccountClosedV1, AccountClosed.format)
        );
    }

    @Override
    public JsonWrite<AccountEvent> eventWrite() {
        return AccountEvent.format;
    }
}