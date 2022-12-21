package com.bts.thoth.bank;

import java.math.BigDecimal;

import static io.vavr.API.Left;
import static io.vavr.API.List;
import static io.vavr.API.Right;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import reactor.core.publisher.Mono;
import fr.maif.eventsourcing.Events;
import fr.maif.eventsourcing.ReactorCommandHandler;
import fr.maif.jooq.reactor.PgAsyncTransaction;

import com.bts.logging.AppLogger;
import com.bts.thoth.bank.BankCommand.CloseAccount;
import com.bts.thoth.bank.BankCommand.Deposit;
import com.bts.thoth.bank.BankCommand.OpenAccount;
import com.bts.thoth.bank.BankCommand.Withdraw;

public class BankCommandHandler implements ReactorCommandHandler<String, Account, BankCommand, BankEvent, Tuple0, PgAsyncTransaction> {
    @Override
    public Mono<Either<String, Events<BankEvent, Tuple0>>> handleCommand(
            PgAsyncTransaction transactionContext,
            Option<Account> previousState,
            BankCommand command) {
        return Mono.fromCallable(() -> switch (command) {
            case Withdraw withdraw -> this.handleWithdraw(previousState, withdraw);
            case Deposit deposit -> this.handleDeposit(previousState, deposit);
            case OpenAccount openAccount -> this.handleOpening(openAccount);
            case CloseAccount close -> this.handleClosing(previousState, close);
        });
    }

    private Either<String, Events<BankEvent, Tuple0>> handleOpening(
            OpenAccount opening) {
        if(opening.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            return Left("Initial balance can't be negative");
        }

        String newId = opening.id().get();
        AppLogger.getLogger().info("Handling new account " + newId + " opening.");
        List<BankEvent> events = List(new BankEvent.AccountOpened(newId));
        if(opening.initialBalance().compareTo(BigDecimal.ZERO) > 0) {
            events = events.append(new BankEvent.MoneyDeposited(newId, opening.initialBalance()));
        }

        return Right(Events.events(events));
    }

    private Either<String, Events<BankEvent, Tuple0>> handleClosing(
            Option<Account> previousState,
            CloseAccount close) {
        AppLogger.getLogger().info("Handling account " + close.id() + " closing.");
        return previousState.toEither("No account opened for this id : " + close.id())
                .map(state ->  Events.events(new BankEvent.AccountClosed(close.id())));
    }

    private Either<String, Events<BankEvent, Tuple0>> handleDeposit(
            Option<Account> previousState,
            Deposit deposit) {
        AppLogger.getLogger().info("Handling account " + deposit.account() + " " + deposit.amount() + "$ money deposit.");
        return previousState.toEither("Account does not exist")
                .map(account -> Events.events(new BankEvent.MoneyDeposited(deposit.account(), deposit.amount())));
    }

    private Either<String, Events<BankEvent, Tuple0>> handleWithdraw(
            Option<Account> previousState,
            Withdraw withdraw) {
        AppLogger.getLogger().info("Handling account " + withdraw.account() + " " + withdraw.amount() + "$ money withdraw.");
        return previousState.toEither("Account does not exist")
                .flatMap(previous -> {
                    BigDecimal newBalance = previous.balance.subtract(withdraw.amount());
                    if(newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Left("Insufficient balance");
                    }
                    return Right(Events.events(new BankEvent.MoneyWithdrawn(withdraw.account(), withdraw.amount())));
                });
    }
}