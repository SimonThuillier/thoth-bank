package com.bts.thoth.bank.account;

import fr.maif.eventsourcing.SimpleCommand;
import io.vavr.Lazy;

import java.math.BigDecimal;

public sealed interface AccountCommand extends SimpleCommand {

    record OpenAccount(Lazy<String> id, BigDecimal initialBalance) implements AccountCommand {
        @Override
        public Lazy<String> entityId() {
            return id;
        }

        @Override
        public Boolean hasId() {
            return false;
        }
    }

    record Deposit(String account, BigDecimal amount) implements AccountCommand {
        @Override
        public Lazy<String> entityId() {
            return Lazy.of(() -> account);
        }
    }

    record Withdraw(String account, BigDecimal amount) implements AccountCommand {
        @Override
        public Lazy<String> entityId() {
            return Lazy.of(() -> account);
        }
    }

    record CloseAccount(String id) implements AccountCommand {
        @Override
        public Lazy<String> entityId() {
            return Lazy.of(() -> id);
        }
    }
}