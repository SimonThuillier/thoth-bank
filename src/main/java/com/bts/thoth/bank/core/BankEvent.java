package com.bts.thoth.bank.core;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import fr.maif.eventsourcing.Event;
import fr.maif.eventsourcing.Type;
import fr.maif.eventsourcing.format.JacksonEventFormat;
import fr.maif.json.Json;
import fr.maif.json.JsonFormat;
import fr.maif.json.JsonRead;
import fr.maif.json.JsonWrite;
import io.vavr.API;
import io.vavr.control.Either;

import static fr.maif.json.Json.$$;
import static fr.maif.json.JsonRead.__;
import static fr.maif.json.JsonRead._bigDecimal;
import static fr.maif.json.JsonRead._string;
import static fr.maif.json.JsonRead.caseOf;
import static fr.maif.json.JsonWrite.$bigdecimal;
import static io.vavr.API.*;

public sealed interface BankEvent extends Event {
    Type<MoneyWithdrawn> MoneyWithdrawnV1 = Type.create(MoneyWithdrawn.class, 1L);
    Type<AccountOpened> AccountOpenedV1 = Type.create(AccountOpened.class, 1L);
    Type<MoneyDeposited> MoneyDepositedV1 = Type.create(MoneyDeposited.class, 1L);
    Type<AccountClosed> AccountClosedV1 = Type.create(AccountClosed.class, 1L);

    JsonFormat<BankEvent> format = JsonFormat.of(
            JsonRead.oneOf(_string("type"),
                    caseOf("MoneyWithdrawn"::equals, MoneyWithdrawn.format),
                    caseOf("AccountOpened"::equals, AccountOpened.format),
                    caseOf("MoneyDeposited"::equals, MoneyDeposited.format),
                    caseOf("AccountClosed"::equals, AccountClosed.format)
            ),
            (BankEvent event) -> switch (event) {
                case MoneyWithdrawn bankEvent -> MoneyWithdrawn.format.write(bankEvent);
                case AccountOpened bankEvent -> AccountOpened.format.write(bankEvent);
                case MoneyDeposited bankEvent -> MoneyDeposited.format.write(bankEvent);
                case AccountClosed bankEvent -> AccountClosed.format.write(bankEvent);
            }
    );

    public static class BankEventJsonParser {

        public Either<String, BankEvent> read(JsonNode json) {
            String eventType = json.has("eventType")? json.get("eventType").textValue(): "NA";

            if(eventType.equals("NA")){
                return Left("Not implemented: eventType not found");
            }

            return switch (eventType) {
                case "AccountOpened" -> Right((BankEvent) Json.fromJson(json, AccountOpened.class).get());
                case "MoneyDeposited" -> Right((BankEvent) Json.fromJson(json, MoneyDeposited.class).get());
                case "MoneyWithdrawn" -> Right((BankEvent) Json.fromJson(json, MoneyWithdrawn.class).get());
                case "AccountClosed" -> Right((BankEvent) Json.fromJson(json, AccountClosed.class).get());
                default -> Left("Not implemented: eventType " + eventType + " unknown");
            };
        }
    }

    public static class BankEventJsonFormat implements JacksonEventFormat<String, BankEvent> {
        @Override
        public Either<String, BankEvent> read(String type, Long version, JsonNode json) {
            return Match(API.Tuple(type, version))
                    .option(
                            Case(AccountOpenedV1.pattern2(), (t, v) -> (BankEvent)Json.fromJson(json, BankEvent.AccountOpened.class).get()),
                            Case(MoneyWithdrawnV1.pattern2(), (t, v) -> (BankEvent)Json.fromJson(json, BankEvent.MoneyWithdrawn.class).get()),
                            Case(MoneyDepositedV1.pattern2(), (t, v) -> (BankEvent)Json.fromJson(json, BankEvent.MoneyDeposited.class).get()),
                            Case(AccountClosedV1.pattern2(), (t, v) -> (BankEvent)Json.fromJson(json, BankEvent.AccountClosed.class).get())
                    )
                    .toEither("Not implemented");
        }

        @Override
        public JsonNode write(BankEvent json) {
            return Json.toJson(json, JsonWrite.auto());
        }
    }

    record MoneyWithdrawn(String accountId, BigDecimal amount) implements BankEvent {

        static class MoneyWithdrawnBuilder{
            String accountId;
            BigDecimal amount;

            MoneyWithdrawnBuilder accountId(String accountId){
                this.accountId = accountId;
                return this;
            }

            MoneyWithdrawnBuilder amount(BigDecimal amount){
                this.amount = amount;
                return this;
            }

            MoneyWithdrawn build(){
                return new MoneyWithdrawn(accountId,amount);
            }

        }

        public static MoneyWithdrawnBuilder builder(){
            return new MoneyWithdrawnBuilder();
        }

        @Override
        public Type<MoneyWithdrawn> type() {
            return MoneyWithdrawnV1;
        }

        @Override
        public String entityId() {
            return accountId;
        }

        public static JsonFormat<MoneyWithdrawn> format = JsonFormat.of(
                __("amount", _bigDecimal(), MoneyWithdrawn.builder()::amount)
                        .and(_string("accountId"), MoneyWithdrawn.MoneyWithdrawnBuilder::accountId)
                        .map(MoneyWithdrawn.MoneyWithdrawnBuilder::build),
                (MoneyWithdrawn moneyWithdrawn) -> Json.obj(
                        $$("type", "MoneyWithdrawn"),
                        $$("amount", moneyWithdrawn.amount, $bigdecimal()),
                        $$("accountId", moneyWithdrawn.accountId)
                )
        );

        public static MoneyWithdrawn parse(JsonNode json){
            return new MoneyWithdrawnBuilder()
                    .accountId(json.get("event").get("accountId").textValue())
                    .amount(BigDecimal.valueOf(json.get("event").get("amount").asDouble()))
                    .build();
        }
    }

    record AccountOpened(String accountId) implements BankEvent {
        static class AccountOpenedBuilder {
            String accountId;

            AccountOpenedBuilder accountId(String accountId){
                this.accountId = accountId;
                return this;
            }

            AccountOpened build(){
                return new AccountOpened(accountId);
            }
        }

        public static AccountOpenedBuilder builder(){
            return new AccountOpenedBuilder();
        }

        @Override
        public Type<AccountOpened> type() {
            return AccountOpenedV1;
        }

        @Override
        public String entityId() {
            return accountId;
        }

        public static JsonFormat<AccountOpened> format = JsonFormat.of(
                __("accountId", _string(), AccountOpened.AccountOpened.builder()::accountId)
                        .map(AccountOpened.AccountOpenedBuilder::build),
                (AccountOpened accountOpened) -> Json.obj(
                        $$("type", "AccountOpened"),
                        $$("accountId", accountOpened.accountId)
                )
        );
    }


    record MoneyDeposited(String accountId, BigDecimal amount) implements BankEvent {

        static class MoneyDepositedBuilder {
            String accountId;
            BigDecimal amount;

            MoneyDepositedBuilder accountId(String accountId){
                this.accountId = accountId;
                return this;
            }

            MoneyDepositedBuilder amount(BigDecimal amount){
                this.amount = amount;
                return this;
            }

            MoneyDeposited build(){
                return new MoneyDeposited(accountId,amount);
            }

        }

        public static MoneyDepositedBuilder builder(){
            return new MoneyDepositedBuilder();
        }

        @Override
        public Type<MoneyDeposited> type() {
            return MoneyDepositedV1;
        }

        @Override
        public String entityId() {
            return accountId;
        }

        public static JsonFormat<MoneyDeposited> format = JsonFormat.of(
                __("accountId", _string(), MoneyDeposited.MoneyDeposited.builder()::accountId)
                        .and(__("amount", _bigDecimal()), MoneyDeposited.MoneyDepositedBuilder::amount)
                        .map(MoneyDeposited.MoneyDepositedBuilder::build),
                (MoneyDeposited moneyDeposited) -> Json.obj(
                        $$("type", "MoneyDeposited"),
                        $$("amount", moneyDeposited.amount, $bigdecimal()),
                        $$("accountId", moneyDeposited.accountId)
                )
        );
    }

    record AccountClosed(String accountId) implements BankEvent {
        static class AccountClosedBuilder{
            String accountId;

            AccountClosedBuilder accountId(String accountId){
                this.accountId = accountId;
                return this;
            }

            AccountClosed build(){
                return new AccountClosed(accountId);
            }
        }

        public static AccountClosedBuilder builder(){
            return new AccountClosedBuilder();
        }

        @Override
        public Type<AccountClosed> type() {
            return AccountClosedV1;
        }

        @Override
        public String entityId() {
            return accountId;
        }


        public static JsonFormat<AccountClosed> format = JsonFormat.of(
                __("accountId", _string(), AccountClosed.AccountClosed.builder()::accountId)
                        .map(AccountClosed.AccountClosedBuilder::build),
                (AccountClosed accountClosed) -> Json.obj(
                        $$("type", "AccountClosed"),
                        $$("accountId", accountClosed.accountId)
                )
        );
    }
}
