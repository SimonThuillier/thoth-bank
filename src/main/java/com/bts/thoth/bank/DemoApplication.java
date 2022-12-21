package com.bts.thoth.bank;

import com.bts.thoth.bank.core.Account;
import com.bts.thoth.bank.core.BankCommandHandler;
import com.bts.thoth.bank.core.BankEventHandler;
import io.vavr.control.Either;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.bts.logging.AppLogger;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static io.vavr.API.Try;
import static io.vavr.API.println;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {

		AppLogger.getLogger().info("Starting app...");

		BankCommandHandler commandHandler = new BankCommandHandler();
		BankEventHandler eventHandler = new BankEventHandler();
		Bank bank = new Bank(commandHandler, eventHandler);

		bank.init()
				.flatMap(__ -> bank.createAccount(BigDecimal.valueOf(100)))
				.flatMap(accountCreatedOrError ->
						accountCreatedOrError
								.fold(
										error -> Mono.just(Either.<String, Account>left(error)),
										currentState -> {
											//String id = currentState.id;
											String id = "lolilol";

											println("account created with id "+id);
											return bank.withdraw(id, BigDecimal.valueOf(500))
													.map(withDrawProcessingResult -> withDrawProcessingResult.map(Account::getBalance))
													.doOnSuccess(balanceOrError ->
															balanceOrError
																	.peek(balance -> println("Balance is now: "+balance))
																	.orElseRun(error -> println("Error: " + error))
													)
													.flatMap(balanceOrError ->
															bank.deposit(id, BigDecimal.valueOf(10))
													)
													.map(depositProcessingResult -> depositProcessingResult.map(Account::getBalance))
													.doOnSuccess(balanceOrError ->
															balanceOrError
																	.peek(balance -> println("Balance is now: "+balance))
																	.orElseRun(error -> println("Error: " + error))
													)
													.flatMap(balanceOrError ->
															bank.findAccountById(id)
													)
													.doOnSuccess(balanceOrError ->
															balanceOrError.forEach(account -> println("Account is: "+account ))
													)
													.flatMap(__ ->
															bank.withdraw(id, BigDecimal.valueOf(25))
													)
													.flatMap(__ ->
															bank.meanWithdrawByClient(id).doOnSuccess(w -> {
																println("Withdraw sum "+w);
															})
													);
										}
								)
				)
				.doOnError(Throwable::printStackTrace)
				.doOnTerminate(() -> {
					Try(() -> {
						bank.close();
						return "";
					});
				})
				.subscribe();
	}

}
