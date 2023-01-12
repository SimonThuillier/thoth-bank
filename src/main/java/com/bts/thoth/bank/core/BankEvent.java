package com.bts.thoth.bank.core;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

import fr.maif.eventsourcing.Event;
import fr.maif.eventsourcing.Type;
import fr.maif.json.Json;
import fr.maif.json.JsonFormat;
import fr.maif.json.JsonRead;

import static fr.maif.json.Json.$$;
import static fr.maif.json.JsonRead.__;
import static fr.maif.json.JsonRead._bigDecimal;
import static fr.maif.json.JsonRead._string;
import static fr.maif.json.JsonRead.caseOf;
import static fr.maif.json.JsonWrite.$bigdecimal;

import com.bts.thoth.bank.core.TransferMode;

public sealed interface BankEvent extends Event {
    Type<AccountOpened> AccountOpenedV1 = Type.create(AccountOpened.class, 1L);
    Type<MoneyDeposited> MoneyDepositedV1 = Type.create(MoneyDeposited.class, 1L);
    Type<MoneyWithdrawn> MoneyWithdrawnV1 = Type.create(MoneyWithdrawn.class, 1L);
    Type<AccountClosed> AccountClosedV1 = Type.create(AccountClosed.class, 1L);

    JsonFormat<BankEvent> format = JsonFormat.of(
            JsonRead.oneOf(_string("type"),
                    caseOf("AccountOpened"::equals, AccountOpened.format),
                    caseOf("MoneyDeposited"::equals, MoneyDeposited.format),
                    caseOf("MoneyWithdrawn"::equals, MoneyWithdrawn.format),
                    caseOf("AccountClosed"::equals, AccountClosed.format)
            ),
            (BankEvent event) -> switch (event) {
                case AccountOpened bankEvent -> AccountOpened.format.write(bankEvent);
                case MoneyDeposited bankEvent -> MoneyDeposited.format.write(bankEvent);
                case MoneyWithdrawn bankEvent -> MoneyWithdrawn.format.write(bankEvent);
                case AccountClosed bankEvent -> AccountClosed.format.write(bankEvent);
            }
    );

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

        public static AccountOpened parse(JsonNode json){
            return new AccountOpened.AccountOpenedBuilder()
                    .accountId(json.get("event").get("accountId").textValue())
                    .build();
        }
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

        public static MoneyDeposited parse(JsonNode json){
            return new MoneyDeposited.MoneyDepositedBuilder()
                    .accountId(json.get("event").get("accountId").textValue())
                    .amount(BigDecimal.valueOf(json.get("event").get("amount").asDouble()))
                    .build();
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

        public static AccountClosed parse(JsonNode json){
            return new AccountClosed.AccountClosedBuilder()
                    .accountId(json.get("event").get("accountId").textValue())
                    .build();
        }
    }
}
