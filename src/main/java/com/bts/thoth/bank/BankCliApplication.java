package com.bts.thoth.bank;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.bts.thoth.bank.core.Account;
import com.bts.thoth.bank.core.BankCommandHandler;
import com.bts.thoth.bank.core.BankEventHandler;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Either;
import io.vavr.control.Option;

import static io.vavr.API.*;
import static java.lang.Thread.sleep;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.Scanner;

@SpringBootApplication
public class BankCliApplication
        implements CommandLineRunner {

    private static final String bannerText = """
            ############
            ## Bank App poc using Event Sourcing data management pattern
            ## Made with thoth : https://github.com/MAIF/thoth
            ############""";

    private static final String menuText = """
            Type a number to select an action :\s
            1 - create new account
            2 - set account for action / consultation
            3 - exit app""";

    private static final String inAccountText =  """
            (Account <account.id> selected)\s
            Type a number to select an action :
            1 - deposit
            2 - withdraw
            3 - get current state
            4 - get history
            5 - close account
            6 - exit account
            7 - exit app
            8 - transfer""";

    private static Logger LOG = LoggerFactory
            .getLogger(BankCliApplication.class);

    public Option<String> getSelectedAccountId() {
        return selectedAccountId;
    }

    public void setSelectedAccountId(Option<String> selectedAccountId) {
        this.selectedAccountId = selectedAccountId;
    }

    private Option<String> selectedAccountId = Option.none();

    static BankCommandHandler commandHandler = new BankCommandHandler();
    static BankEventHandler eventHandler = new BankEventHandler();
    static Bank bank = new Bank(commandHandler, eventHandler);

    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");

        Disposable task = bank
                .init()
                .doOnError(Throwable::printStackTrace)
                .doOnTerminate(() -> {
                    LOG.info("Thoth bank backend successfully initialized");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}

        SpringApplication.run(BankCliApplication.class, args);
        LOG.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) {
        LOG.info("EXECUTING : command line runner");

         //System.in is a standard input stream.

        System.out.println(bannerText);
        System.out.println();

        if(selectedAccountId.isEmpty()){
            runPrimaryMenu();
        }
        // case not empty inAccount
    }

    private void runPrimaryMenu(){

        int selectedAction = 0;
        System.out.println(menuText);
        Scanner sc= new Scanner(System.in);
        String input;
        Pattern actionPattern = Pattern.compile("^([1-3])$");
        Matcher actionMatcher;

        int accountMenuAction;

        while (selectedAction != 3){
            if(selectedAction != -1){
                System.out.print("Type a choice and enter to validate: ");
            }
            else {
                System.out.print("Invalid input. Please enter a valid action number: ");
            }

            selectedAction = -1;
            input = sc.nextLine().trim();
            actionMatcher = actionPattern.matcher(input);
            if (actionMatcher.matches()){
                selectedAction = Integer.parseInt(actionMatcher.group(1));
            }

            switch(selectedAction){
                case 1:
                    this.createNewAccount();
                    accountMenuAction = this.runAccountMenu();
                    if(accountMenuAction == 7){
                        // since in account menu action user chose to exit the application
                        selectedAction = 3;
                        break;
                    }
                    System.out.println("--> back to main menu -->");
                    selectedAction = 0;
                    System.out.println(menuText);
                    break;
                case 2:
                    if(this.setAccount()){
                        accountMenuAction = this.runAccountMenu();
                        if(accountMenuAction == 7){
                            // since in account menu action user chose to exit the application
                            selectedAction = 3;
                            break;
                        }
                        System.out.println("--> back to main menu -->");
                        selectedAction = 0;
                        System.out.println(menuText);
                        break;
                    }
                    selectedAction = 0;
                    break;
                case 3:
                    // main menu loop will exit
                    break;
                case -1:
                    break;
            }
        }

        try {
            bank.close();
            LOG.info("Thoth bank backend successfully closed.");
        } catch (IOException e) {
            LOG.warn("Error closing Thoth bank backend : ", e);
        }

        System.out.println("Goodbye !");
    }

    private int runAccountMenu(){

        // by security this menu can be executed only if selectedAccountId is set
        if (selectedAccountId.isEmpty()){
            return 0;
        }

        int selectedAction = 0;
        System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
        Scanner sc= new Scanner(System.in);
        String input;
        Pattern actionPattern = Pattern.compile("^([1-8])$");
        Matcher actionMatcher;

        while (selectedAction != 6 && selectedAction != 7 && selectedAccountId.isDefined()){
            if(selectedAction != -1){
                System.out.print("Type a choice and enter to validate: ");
            }
            else {
                System.out.print("Invalid input. Please enter a valid action number: ");
            }

            selectedAction = -1;
            input = sc.nextLine().trim();
            actionMatcher = actionPattern.matcher(input);
            if (actionMatcher.matches()){
                selectedAction = Integer.parseInt(actionMatcher.group(1));
            }

            switch(selectedAction){
                case 1:
                    this.deposit();
                    selectedAction = 0;
                    System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 2:
                    this.withdraw();
                    selectedAction = 0;
                    System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 3:
                    this.getAccountInfo();
                    selectedAction = 0;
                    System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 4:
                    // TODO get history
                    selectedAction = 0;
                    System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 5:
                    if(this.close()){
                        // with account closed we exit it
                        selectedAction = 6;
                        selectedAccountId = Option.none();
                    }
                    else {
                        selectedAction = 0;
                        System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    }
                    break;
                case 6, 7:
                    selectedAccountId = Option.none();
                    // account menu loop will exit
                    break;
                case 8:
                    this.transfer();
                    selectedAction = 0;
                    System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case -1:
                    break;
            }


        }
        return selectedAction;
    }



    private void createNewAccount(){

        System.out.println("You will begin a new account creation process. Type 'cancel' and enter at any moment to cancel.");
        System.out.print("Type an initial deposit (ex 10.5) and enter to validate: ");

        Option<BigDecimal> initialDeposit = requestPositiveAmountOrCancel();
        if(initialDeposit.isEmpty()){return;} // action was canceled

        BigDecimal finalInitialDeposit = initialDeposit.get();
        System.out.print("Creating new account...");

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.createAccount(finalInitialDeposit))
                .flatMap(accountCreatedOrError ->
                         accountCreatedOrError
                                .fold(
                                        error -> Mono.just(Either.<String, Account>left(error)),
                                        currentState -> {
                                            selectedAccountId = Option.some(currentState.id);
                                            return Mono.empty();
                                        }
                                )
                )
                .doOnError(Throwable::printStackTrace)
                .doOnTerminate(() -> {
                    System.out.println("successfully done !");
                    System.out.println("Account " + selectedAccountId + " created.");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private boolean setAccount(){

        System.out.print("Type account ID to set or 'cancel' and enter to validate: ");

        Option<String> accountId = requestChoicesOrCancel(Option.none());
        if(accountId.isEmpty()){return false;} // action was canceled

        // TODO : request backend to see if this accountId exists

        selectedAccountId = accountId;
        return true;
    }

    private void deposit(){

        System.out.print("Type amount to deposit (ex 10.5) or 'cancel' to abort, and enter to validate: ");

        Option<BigDecimal> deposit = requestPositiveAmountOrCancel();
        if(deposit.isEmpty()){return;} // action was canceled

        BigDecimal finalDeposit = deposit.get();
        System.out.print("Depositing..");

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.deposit(selectedAccountId.get(), finalDeposit))
                .doOnError(Throwable::printStackTrace)
                .doOnTerminate(() -> {
                    System.out.println("successfully done !");
                    System.out.println(finalDeposit + " deposited on account " + selectedAccountId.get() + ".");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private void withdraw(){

        System.out.print("Type amount to withdraw (ex 10.5) or 'cancel' to abort, and enter to validate: ");

        Option<BigDecimal> withdraw = requestPositiveAmountOrCancel();
        if(withdraw.isEmpty()){return;} // action was cancelled

        BigDecimal finalWithdraw = withdraw.get();
        System.out.print("Withdrawing...");

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.withdraw(selectedAccountId.get(), finalWithdraw))
                .flatMap(errorOrState ->
                        errorOrState
                                .fold(
                                        (e) -> {
                                            println("ERROR : " + e);
                                            return Mono.from(Flux.range(0, 0));
                                        },
                                        account -> {
                                            System.out.println("successfully done !");
                                            System.out.println("New balance is: " + account.getBalance());
                                            return Mono.from(Flux.range(0, 0));
                                        }
                                )
                )
                .doOnError(Throwable::printStackTrace)
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private void transfer(){

        System.out.print("Type amount to transfer (ex 10.5) or 'cancel' to abort, and enter to validate: ");
        Option<BigDecimal> transfer = requestPositiveAmountOrCancel();
        if(transfer.isEmpty()){return;} // action was cancelled

        System.out.print("Type the target account Id where to transfer money or 'cancel' to abort, and enter to validate: ");
        Option<String> targetAccountId = requestChoicesOrCancel(Option.none());
        if(targetAccountId.isEmpty()){return;} // action was cancelled

        BigDecimal finalTransfer = transfer.get();
        String finalTargetAccountId = targetAccountId.get();
        System.out.print("Transferring...");

        Option<BigDecimal> originAccountNewBalance = Option.none();

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.withdraw(selectedAccountId.get(), finalTransfer))
                .flatMap(errorOrState ->
                        errorOrState
                                .fold(
                                        (e) -> {
                                            println("ERROR : " + e);
                                            return Mono.from(Flux.range(0, 0));
                                        },
                                        account -> {
                                            originAccountNewBalance.transform(__ -> Option.some(account.getBalance()));
                                            return Mono.from(Flux.range(0, 0));
                                        }
                                )
                )
                .flatMap(__ -> bank.deposit(finalTargetAccountId, finalTransfer))
                .flatMap(errorOrState ->
                        errorOrState
                                .fold(
                                        (e) -> {
                                            println("ERROR : " + e);
                                            return Mono.from(Flux.range(0, 0));
                                        },
                                        account -> {
                                            System.out.println("successfully done !");
                                            System.out.println("After transfer to account " + finalTargetAccountId + " my new balance is: " + originAccountNewBalance.get());
                                            return Mono.from(Flux.range(0, 0));
                                        }
                                )
                )
                .doOnError(Throwable::printStackTrace)
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private void getAccountInfo(){

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.findAccountById(selectedAccountId.get()))
                .doOnSuccess(statesOrError ->

                        statesOrError.fold(
                                () -> {
                                    println("ERROR : no opened account with ID " + selectedAccountId.get());
                                    return Mono.from(Flux.range(0, 0));
                                },
                                account -> {
                                    print("""
                                    ################################################
                                    Current informations for account <account.id>
                                    ################################################
                                    """.replaceFirst("<account.id>", account.getId()));

                                    println("Account balance: " + account.getBalance() );
                                    println("Sequence number: " + account.getSequenceNum() );
                                    println("________________________________________________");
                                    return null;
                                }
                        )
                        )
                .doOnError(Throwable::printStackTrace)
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private boolean close(){

        System.out.print("You will close definitely account " + selectedAccountId.get() + ". Are you sure ? (type 'yes' or 'cancel'): ");

        Option<String> response = requestChoicesOrCancel(Option.some(List.of("yes")));
        if(response.isEmpty()){return false;} // action was cancelled

        System.out.print("Closing..");

        // since lambda in generate should not update outer-scope objects to prevent side effects
        // we will put validation var in the hashmap behind and update it in lambda
        Map<String, Boolean> resultStorage = new HashMap<String, Boolean>();

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.close(selectedAccountId.get()))
                .doOnError(Throwable::printStackTrace)
                .doOnTerminate(() -> {
                    System.out.println("successfully done !");
                    resultStorage.put("accountClosed", true);
                    System.out.println("Account " + selectedAccountId.get() + " was closed.");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
        return resultStorage.containsKey("accountClosed");
    }

    private Option<BigDecimal> requestPositiveAmountOrCancel(){

        Option<BigDecimal> initialDeposit = Option.none();
        Scanner sc= new Scanner(System.in);
        String input;

        while (initialDeposit.isEmpty()) {
            input = sc.nextLine().trim();
            if(input.equalsIgnoreCase("CANCEL")){
                return initialDeposit; // which means : to abort ongoing action
            }

            Pattern amountPattern = Pattern.compile("^\\d+(\\.\\d+)?$");
            Matcher amountMatcher = amountPattern.matcher(input);
            if (!amountMatcher.matches()){
                System.out.print("Invalid amount. Please enter a valid amount (ex 10.5): ");
                continue;
            }
            if (BigDecimal.valueOf(Double.parseDouble(input)).compareTo(BigDecimal.ZERO) <= 0){
                System.out.print("Amount must be strictly superior to 0: ");
                continue;
            }
            // if entry is ok
            initialDeposit = Option.some(BigDecimal.valueOf(Double.parseDouble(input)));
        }
        return initialDeposit;
    }

    private Option<String> requestChoicesOrCancel(Option<List<String>> choices){

        Option<String> response = Option.none();
        Scanner sc= new Scanner(System.in);
        String input;

        while (response.isEmpty()) {
            input = sc.nextLine().trim();
            if(input.equalsIgnoreCase("CANCEL")){
                return response; // which means : to abort ongoing action
            }
            if (choices.isEmpty()){
                response = Option.some(input);
                return response;
            }
            // case with choices defined
            for(String choice: choices.get()){
                if(input.equalsIgnoreCase(choice)){
                    // first input matching choice is returned
                    return Option.some(choice);
                }
            }
            // invalid answer
            System.out.print("Please type 'cancel' or ["+ choices.get().mkString("'","|","'") +"] : ");
        }
        return response;
    }
}