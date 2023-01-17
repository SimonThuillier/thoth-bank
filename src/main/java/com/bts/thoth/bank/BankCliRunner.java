package com.bts.thoth.bank;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.vavr.API.print;
import static io.vavr.API.println;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.bts.thoth.bank.account.Account;
import com.bts.thoth.bank.repository.AccountHistoryRepository;
import com.bts.thoth.bank.repository.AccountRepository;

@Component
public class BankCliRunner implements CommandLineRunner {

    /**
     * TIMEOUT for asynchronous tasks to complete in milliseconds
     */
    private static final long TIMEOUT = 3000L;
    private static final String bannerText = """
            ############
            ## Bank App poc using Event Sourcing data management pattern
            ## Made with thoth : https://github.com/MAIF/thoth
            ############""";

    private static final String menuText = """
            Type a number to select an action :\s
            1 - create new account
            2 - set account for action / consultation
            3 - get one account information
            4 - get one account history
            5 - find last opened accounts
            6 - find first opened accounts
            7 - find last closed accounts
            8 - find first closed accounts
            9 - exit app""";

    private static final String inAccountText =  """
            (Account <account.id> selected)\s
            Type a number to select an action :
            1 - deposit
            2 - withdraw
            3 - get current state
            4 - get history
            5 - close account
            6 - exit account
            7 - exit app""";
    // 8 - transfer TODO
    private Option<String> selectedAccountId = Option.none();
    private final Bank bank;
    private final AccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;

    public BankCliRunner(Bank bank,
                         AccountRepository accountRepository,
                         AccountHistoryRepository accountHistoryRepository) {

        this.bank = bank;
        this.accountRepository = accountRepository;
        this.accountHistoryRepository = accountHistoryRepository;
    }

    @Override
    public void run(String... args) {
        LoggerFactory.getLogger("Bank").info("STARTING THE APPLICATION");
        LoggerFactory.getLogger("Bank").info("EXECUTING : command line runner");

        println(bannerText);
        println();

        if(selectedAccountId.isEmpty()){
            runPrimaryMenu();
        }
        // case not empty inAccount
    }

    private void runPrimaryMenu(){

        int selectedAction = 0;
        println(menuText);
        Scanner sc= new Scanner(System.in);
        String input;
        Pattern actionPattern = Pattern.compile("^([1-9])$");
        Matcher actionMatcher;

        int accountMenuAction;

        while (selectedAction != 9){
            if(selectedAction != -1){
                print("Type a choice and enter to validate: ");
            }
            else {
                print("Invalid input. Please enter a valid action number: ");
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
                    println("--> back to main menu -->");
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 2:
                    if(this.setAccount()){
                        accountMenuAction = this.runAccountMenu();
                        if(accountMenuAction == 7){
                            // since in account menu action user chose to exit the application
                            selectedAction = 9;
                            break;
                        }
                        printWithSeparation("--> back to main menu -->");
                        selectedAction = 0;
                        println(menuText);
                        break;
                    }
                    selectedAction = 0;
                    break;
                case 3:
                    this.getOneAccountInfo();
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 4:
                    this.getOneAccountHistory();
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 5:
                    this.findAccounts("OPEN", "DESC");
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 6:
                    this.findAccounts("OPEN", "ASC");
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 7:
                    this.findAccounts("CLOSED", "DESC");
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 8:
                    this.findAccounts("CLOSED", "ASC");
                    selectedAction = 0;
                    println(menuText);
                    break;
                case 9:
                    // main menu loop will exit
                    break;
                case -1:
                    break;
            }
        }

        try {
            bank.close();
            LoggerFactory.getLogger("Bank").info("Thoth bank backend successfully closed.");
        } catch (IOException e) {
            LoggerFactory.getLogger("Bank").warn("Error closing Thoth bank backend : ", e);
        }

        println("Goodbye !");
    }

    private int runAccountMenu(){

        // by security this menu can be executed only if selectedAccountId is set
        if (selectedAccountId.isEmpty()){
            return 0;
        }

        int selectedAction = 0;
        printWithSeparation(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
        Scanner sc= new Scanner(System.in);
        String input;
        Pattern actionPattern = Pattern.compile("^([1-8])$");
        Matcher actionMatcher;

        while (selectedAction != 6 && selectedAction != 7 && selectedAccountId.isDefined()){
            if(selectedAction != -1){
                print("Type a choice and enter to validate: ");
            }
            else {
                print("Invalid input. Please enter a valid action number: ");
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
                    println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 2:
                    this.withdraw();
                    selectedAction = 0;
                    println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 3:
                    this.getSelectedAccountInfo();
                    selectedAction = 0;
                    println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 4:
                    this.getSelectedAccountHistory();
                    selectedAction = 0;
                    println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case 5:
                    if(this.close()){
                        // with account closed we exit it
                        selectedAction = 6;
                        selectedAccountId = Option.none();
                    }
                    else {
                        selectedAction = 0;
                        println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    }
                    break;
                case 6, 7:
                    selectedAccountId = Option.none();
                    // account menu loop will exit
                    break;
                case 8:
                    this.transfer();
                    selectedAction = 0;
                    println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get()));
                    break;
                case -1:
                    break;
            }
        }
        return selectedAction;
    }

    private void createNewAccount(){

        println("You will begin a new account creation process. Type 'cancel' and enter at any moment to cancel.");
        print("Type an initial deposit (ex 10.5) and enter to validate: ");

        Option<BigDecimal> initialDeposit = requestPositiveAmountOrCancel();
        if(initialDeposit.isEmpty()){return;} // action was canceled

        BigDecimal finalInitialDeposit = initialDeposit.get();
        print("Creating new account...");

        CountDownLatch myLatch = new CountDownLatch(1);
        Mono.from(Flux.range(0, 1))
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
                .doOnError(error -> {
                    println("ERROR : " + error);
                    myLatch.countDown();
                })
                .doOnTerminate(() -> {
                    println("successfully done !");
                    printWithSeparation("Account " + selectedAccountId.get() + " created.");
                    myLatch.countDown();
                })
                .subscribe();
        try {myLatch.await(3000L, TimeUnit.MILLISECONDS);} catch (InterruptedException ignored) {}
    }

    private boolean setAccount(){

        print("Type account ID to set or 'cancel' and enter to validate: ");
        Option<String> accountId = requestChoicesOrCancel(Option.none());
        if(accountId.isEmpty()){return false;} // action was canceled

        // TODO : request backend to see if this accountId exists
        selectedAccountId = accountId;
        return true;
    }

    private void deposit(){

        print("Type amount to deposit (ex 10.5) or 'cancel' to abort, and enter to validate: ");

        Option<BigDecimal> deposit = requestPositiveAmountOrCancel();
        if(deposit.isEmpty()){return;} // action was canceled

        BigDecimal finalDeposit = deposit.get();
        print("Depositing..");

        CountDownLatch myLatch = new CountDownLatch(1);
        Mono.from(Flux.range(0, 1))
                .flatMap(__ -> bank.deposit(selectedAccountId.get(), finalDeposit))
                .doOnError(error -> {
                    println("ERROR : " + error);
                    myLatch.countDown();
                })
                .doOnTerminate(() -> {
                    println("successfully done !");
                    printWithSeparation(finalDeposit + " deposited on account " + selectedAccountId.get() + ".");
                    myLatch.countDown();
                })
                .subscribe();
        try {myLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);} catch (InterruptedException ignored) {}
    }

    private void withdraw(){

        print("Type amount to withdraw (ex 10.5) or 'cancel' to abort, and enter to validate: ");

        Option<BigDecimal> withdraw = requestPositiveAmountOrCancel();
        if(withdraw.isEmpty()){return;} // action was cancelled

        BigDecimal finalWithdraw = withdraw.get();
        print("Withdrawing...");

        CountDownLatch myLatch = new CountDownLatch(1);
        Mono.from(Flux.range(0, 1))
                .flatMap(__ -> bank.withdraw(selectedAccountId.get(), finalWithdraw))
                .flatMap(errorOrState ->
                        errorOrState
                                .fold(
                                        (e) -> {
                                            println("ERROR : " + e);
                                            return Mono.from(Flux.range(0, 1));
                                        },
                                        account -> {
                                            println("successfully done !");
                                            printWithSeparation("New balance is: " + account.getBalance());
                                            return Mono.from(Flux.range(0, 1));
                                        }
                                )
                )
                .subscribe(
                        __ -> myLatch.countDown(),
                        error -> myLatch.countDown()
                );
        try {myLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);} catch (InterruptedException ignored) {}
    }

    private void transfer(){

        print("Type amount to transfer (ex 10.5) or 'cancel' to abort, and enter to validate: ");
        Option<BigDecimal> transfer = requestPositiveAmountOrCancel();
        if(transfer.isEmpty()){return;} // action was cancelled

        print("Type the target account Id where to transfer money or 'cancel' to abort, and enter to validate: ");
        Option<String> targetAccountId = requestChoicesOrCancel(Option.none());
        if(targetAccountId.isEmpty()){return;} // action was cancelled

        BigDecimal finalTransfer = transfer.get();
        String finalTargetAccountId = targetAccountId.get();
        print("Transferring...");

        Option<BigDecimal> originAccountNewBalance = Option.none();

        CountDownLatch myLatch = new CountDownLatch(1);
        Mono.from(Flux.range(0, 1))
                .flatMap(__ -> bank.withdraw(selectedAccountId.get(), finalTransfer))
                .flatMap(errorOrState ->
                        errorOrState
                                .fold(
                                        (e) -> {
                                            println("ERROR : " + e);
                                            myLatch.countDown();
                                            return Mono.empty();
                                        },
                                        account -> {
                                            originAccountNewBalance.transform(__ -> Option.some(account.getBalance()));
                                            return Mono.just(1);
                                        }
                                )
                )
                .flatMap(__ -> bank.deposit(finalTargetAccountId, finalTransfer))
                .flatMap(errorOrState ->
                        errorOrState
                                .fold(
                                        (e) -> {
                                            println("ERROR : " + e);
                                            return Mono.just(1);
                                        },
                                        account -> {
                                            println("successfully done !");
                                            printWithSeparation("After transfer to account " + finalTargetAccountId + " my new balance is: " + originAccountNewBalance.get());
                                            return Mono.just(1);
                                        }
                                )
                )
                .subscribe(
                        __ -> myLatch.countDown(),
                        error -> myLatch.countDown()
                );

        try {myLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);} catch (InterruptedException ignored) {}
    }

    private boolean getOneAccountInfo(){
        print("Type id of the account you want to consult or 'cancel': ");
        Option<String> response = requestChoicesOrCancel(Option.none());
        if(response.isEmpty()){return false;} // action was cancelled
        getAccountInfo(response.get());
        return true;
    }

    private void getSelectedAccountInfo(){
        getAccountInfo(selectedAccountId.get());
    }

    private void getAccountInfo(String accountId){

        println("Getting account info...");
        CountDownLatch myLatch = new CountDownLatch(1);
        accountRepository.getById(accountId)
                .subscribe(
                        msg -> {
                            printWithSeparation(msg);
                            myLatch.countDown();
                        },
                        error -> {
                            println("ERROR : " + error);
                            myLatch.countDown();
                        }
                );
        try {myLatch.await(3000L, TimeUnit.MILLISECONDS);} catch (InterruptedException e) {return;}
    }

    private boolean getOneAccountHistory(){
        print("Type id of the account you want to consult or 'cancel': ");
        Option<String> response = requestChoicesOrCancel(Option.none());
        if(response.isEmpty()){return false;} // action was cancelled
        getAccountHistory(response.get());
        return true;
    }

    private boolean getSelectedAccountHistory(){
        return getAccountHistory(selectedAccountId.get());
    }

    private boolean getAccountHistory(String accountId){

        print("Type maximum history items number to retrieve or 'cancel': ");
        Option<BigDecimal> response = requestPositiveAmountOrCancel();
        if(response.isEmpty()){return false;} // action was cancelled
        Integer count = response.get().setScale(0, RoundingMode.HALF_UP).intValueExact();

        println("Getting account history...");

        CountDownLatch myLatch = new CountDownLatch(1);
        accountHistoryRepository.getHistoryByAccountId2(accountId, count)
                .subscribe(
                        msg -> {
                            printWithSeparation(msg);
                            myLatch.countDown();
                        },
                        error -> {
                            println("ERROR : " + error);
                            myLatch.countDown();
                        }
                );
        try {myLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);} catch (InterruptedException e) {return false;}

        return true;
    }

    private boolean findAccounts(String status, String sense){

        print("Type the number of accounts to search or 'cancel': ");
        Option<BigDecimal> response = requestPositiveAmountOrCancel();
        if(response.isEmpty()){return false;} // action was cancelled
        Integer count = response.get().setScale(0, RoundingMode.HALF_UP).intValueExact();

        String message = "Getting " + count + " "
                + (Objects.equals(sense, "DESC") ?"last":"first")
                + " " + (Objects.equals(status, "OPEN") ? "open":"closed") + " accounts...";
        println(message);

        CountDownLatch myLatch = new CountDownLatch(1);
        accountRepository.findAccountIds(status, sense, count)
                .subscribe(
                        msg -> {
                            printWithSeparation(msg);
                            myLatch.countDown();
                        },
                        error -> {
                            println("ERROR : " + error);
                            myLatch.countDown();
                        }
                );
        try {myLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);} catch (InterruptedException e) {return false;}

        return true;
    }

    private boolean close(){

        print("You will close definitely account " + selectedAccountId.get() + ". Are you sure ? (type 'yes' or 'cancel'): ");
        Option<String> response = requestChoicesOrCancel(Option.some(List.of("yes")));
        if(response.isEmpty()){return false;} // action was cancelled
        print("Closing account...");

        // since lambda in generate should not update outer-scope objects to prevent side effects
        // we will put validation var in the hashmap behind and update it in lambda
        Map<String, Boolean> resultStorage = new HashMap<String, Boolean>();

        CountDownLatch myLatch = new CountDownLatch(1);
        Mono.from(Flux.range(0, 1))
                .flatMap(__ -> bank.close(selectedAccountId.get()))
                .subscribe(
                        msg -> {
                            println("successfully done !");
                            resultStorage.put("accountClosed", true);
                            printWithSeparation("Account " + selectedAccountId.get() + " was closed.");
                            myLatch.countDown();
                        },
                        error -> {
                            println("ERROR : " + error);
                            myLatch.countDown();
                        }
                );
        try {myLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);} catch (InterruptedException e) {return false;}
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
                print("Invalid amount. Please enter a valid amount (ex 10.5): ");
                continue;
            }
            if (BigDecimal.valueOf(Double.parseDouble(input)).compareTo(BigDecimal.ZERO) <= 0){
                print("Amount must be strictly superior to 0: ");
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
            print("Please type 'cancel' or ["+ choices.get().mkString("'","|","'") +"] : ");
        }
        return response;
    }

    static void printWithSeparation(String msg){
        println("###########################################");
        println(msg);
        println("___________________________________________");
    }
}