package com.bts.thoth.bank;

import com.bts.thoth.bank.account.Account;
import io.vavr.control.Either;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static io.vavr.API.Try;
import static io.vavr.API.println;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {

		ApplicationContext context = new AnnotationConfigApplicationContext(
				"com.bts.thoth.bank", "com.bts.thoth.bank.config");

		Bank bank = context.getBean(Bank.class);

		bank.init()
				.flatMap(__ -> bank.createAccount(BigDecimal.valueOf(100)))
				.flatMap(accountCreatedOrError ->
						accountCreatedOrError
								.fold(
										error -> Mono.just(Either.<String, Account>left(error)),
										currentState -> {
											String id = currentState.id;
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
