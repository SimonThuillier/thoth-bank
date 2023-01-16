package com.bts.thoth.bank.account;

import java.math.BigDecimal;

import io.vavr.control.Option;
import fr.maif.eventsourcing.EventHandler;

import com.bts.thoth.bank.account.AccountEvent.AccountClosed;
import com.bts.thoth.bank.account.AccountEvent.AccountOpened;
import com.bts.thoth.bank.account.AccountEvent.MoneyDeposited;
import com.bts.thoth.bank.account.AccountEvent.MoneyWithdrawn;
import org.springframework.stereotype.Component;

@Component
public class AccountEventHandler implements EventHandler<Account, AccountEvent> {
    @Override
    public Option<Account> applyEvent(
            Option<Account> previousState,
            AccountEvent event) {
        return switch (event) {
            case AccountOpened accountOpened -> AccountEventHandler.handleAccountOpened(accountOpened);
            case MoneyDeposited deposit -> AccountEventHandler.handleMoneyDeposited(previousState, deposit);
            case MoneyWithdrawn deposit -> AccountEventHandler.handleMoneyWithdrawn(previousState, deposit);
            case AccountClosed accountClosed -> AccountEventHandler.handleAccountClosed(accountClosed);
        };
    }

    private static Option<Account> handleAccountClosed(
            AccountClosed close) {
        return Option.none();
    }

    private static Option<Account> handleMoneyWithdrawn(
            Option<Account> previousState,
            MoneyWithdrawn withdraw) {
        return previousState.map(account -> {
            account.balance = account.balance.subtract(withdraw.amount());
            return account;
        });
    }

    private static Option<Account> handleAccountOpened(AccountOpened event) {
        Account account = new Account();
        account.id = event.accountId();
        account.balance = BigDecimal.ZERO;

        return Option.some(account);
    }

    private static Option<Account> handleMoneyDeposited(
            Option<Account> previousState,
            MoneyDeposited event) {
        return previousState.map(state -> {
            state.balance = state.balance.add(event.amount());
            return state;
        });
    }
}