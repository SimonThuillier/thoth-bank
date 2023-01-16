package com.bts.thoth.bank.account;

import java.math.BigDecimal;

import static io.vavr.API.Left;
import static io.vavr.API.List;
import static io.vavr.API.Right;

import io.vavr.Tuple0;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import fr.maif.eventsourcing.Events;
import fr.maif.eventsourcing.ReactorCommandHandler;
import fr.maif.jooq.reactor.PgAsyncTransaction;

import com.bts.thoth.bank.account.AccountCommand.CloseAccount;
import com.bts.thoth.bank.account.AccountCommand.Deposit;
import com.bts.thoth.bank.account.AccountCommand.OpenAccount;
import com.bts.thoth.bank.account.AccountCommand.Withdraw;

@Component
public class AccountCommandHandler implements ReactorCommandHandler<String, Account, AccountCommand, AccountEvent, Tuple0, PgAsyncTransaction> {
    @Override
    public Mono<Either<String, Events<AccountEvent, Tuple0>>> handleCommand(
            PgAsyncTransaction transactionContext,
            Option<Account> previousState,
            AccountCommand command) {
        return Mono.fromCallable(() -> switch (command) {
            case Withdraw withdraw -> this.handleWithdraw(previousState, withdraw);
            case Deposit deposit -> this.handleDeposit(previousState, deposit);
            case OpenAccount openAccount -> this.handleOpening(openAccount);
            case CloseAccount close -> this.handleClosing(previousState, close);
        });
    }

    private Either<String, Events<AccountEvent, Tuple0>> handleOpening(
            OpenAccount opening) {
        if(opening.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            return Left("Initial balance can't be negative");
        }

        String newId = opening.id().get();
        LoggerFactory.getLogger("Bank").info("Handling new account " + newId + " opening.");
        List<AccountEvent> events = List(new AccountEvent.AccountOpened(newId));
        if(opening.initialBalance().compareTo(BigDecimal.ZERO) > 0) {
            events = events.append(new AccountEvent.MoneyDeposited(newId, opening.initialBalance()));
        }

        return Right(Events.events(events));
    }

    private Either<String, Events<AccountEvent, Tuple0>> handleClosing(
            Option<Account> previousState,
            CloseAccount close) {

        if(previousState.isEmpty()) {
            return Left("This account doesn't exist.");
        }

        LoggerFactory.getLogger("Account").info("Handling account " + close.id() + " closing.");
        List<AccountEvent> events = List(new AccountEvent.AccountClosed(close.id()));
        if(previousState.get().getBalance().compareTo(BigDecimal.ZERO) > 0) {
            events = events.prepend(new AccountEvent.MoneyWithdrawn(close.id(), previousState.get().getBalance()));
        }

        return Right(Events.events(events));
    }

    private Either<String, Events<AccountEvent, Tuple0>> handleDeposit(
            Option<Account> previousState,
            Deposit deposit) {
        LoggerFactory.getLogger("Account").info("Handling account " + deposit.account() + " " + deposit.amount() + "$ money deposit.");
        return previousState.toEither("Account does not exist")
                .map(account -> Events.events(new AccountEvent.MoneyDeposited(deposit.account(), deposit.amount())));
    }

    private Either<String, Events<AccountEvent, Tuple0>> handleWithdraw(
            Option<Account> previousState,
            Withdraw withdraw) {
        LoggerFactory.getLogger("Account").info("Handling account " + withdraw.account() + " " + withdraw.amount() + "$ money withdraw.");
        return previousState.toEither("Account does not exist")
                .flatMap(previous -> {
                    BigDecimal newBalance = previous.balance.subtract(withdraw.amount());
                    if(newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Left("Insufficient balance");
                    }
                    return Right(Events.events(new AccountEvent.MoneyWithdrawn(withdraw.account(), withdraw.amount())));
                });
    }
}