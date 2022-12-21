package com.bts.thoth.bank.core;

import com.bts.thoth.bank.core.BankEvent.*;
import fr.maif.eventsourcing.Type;
import fr.maif.json.EventEnvelopeJsonFormat;
import fr.maif.json.JsonRead;
import fr.maif.json.JsonWrite;
import io.vavr.Tuple0;
import io.vavr.Tuple2;
import io.vavr.collection.List;

import static io.vavr.API.List;
import static io.vavr.API.Tuple;

public class BankEventFormat implements EventEnvelopeJsonFormat<BankEvent, Tuple0, Tuple0> {

    public static BankEventFormat bankEventFormat = new BankEventFormat();

    @Override
    public List<Tuple2<Type<? extends BankEvent>, JsonRead<? extends BankEvent>>> cases() {
        return List(
                Tuple(BankEvent.MoneyWithdrawnV1, MoneyWithdrawn.format),
                Tuple(BankEvent.AccountOpenedV1, AccountOpened.format),
                Tuple(BankEvent.MoneyDepositedV1, MoneyDeposited.format),
                Tuple(BankEvent.AccountClosedV1, AccountClosed.format)
        );
    }

    @Override
    public JsonWrite<BankEvent> eventWrite() {
        return BankEvent.format;
    }
}