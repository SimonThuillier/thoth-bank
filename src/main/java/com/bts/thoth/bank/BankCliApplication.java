package com.bts.thoth.bank;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.bts.thoth.bank.core.Account;
import com.bts.thoth.bank.core.BankCommandHandler;
import com.bts.thoth.bank.core.BankEventHandler;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.control.Either;
import io.vavr.control.Option;

import static io.vavr.API.*;
import static io.vavr.Predicates.*;
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
            7 - exit app""";

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
                    int accountMenuAction = this.runAccountMenu();
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
                    // this.setAccount();
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
        System.out.println(inAccountText.replaceFirst("<account.id>", selectedAccountId.get().toString()));
        Scanner sc= new Scanner(System.in);
        String input;
        Pattern actionPattern = Pattern.compile("^([1-7])$");
        Matcher actionMatcher;

        while (selectedAction != 6 && selectedAction != 7){
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
                    selectedAction = 0;
                    System.out.println(menuText);
                    break;
                case 6, 7:
                    selectedAccountId = Option.none();
                    // account menu loop will exit
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
                    System.out.println(finalDeposit + "$  deposited on account " + selectedAccountId + ".");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private void withdraw(){

        System.out.print("Type amount to withdraw (ex 10.5) or 'cancel' to abort, and enter to validate: ");

        Option<BigDecimal> withdraw = requestPositiveAmountOrCancel();
        if(withdraw.isEmpty()){return;} // action was cancelled

        BigDecimal finalWithdraw = withdraw.get();
        System.out.print("Depositing..");

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.deposit(selectedAccountId.get(), finalWithdraw))
                .doOnError(Throwable::printStackTrace)
                .doOnTerminate(() -> {
                    System.out.println("successfully done !");
                    System.out.println(finalWithdraw + "$  deposited on account " + selectedAccountId + ".");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
    }

    private void close(){

        System.out.print("You will close definitely account " + selectedAccountId + ". Are you sure ? (type 'yes' or 'cancel' : ");

        Option<String> response = requestYesOrCancel();
        if(response.isEmpty()){return;} // action was cancelled

        System.out.print("Closing..");

        Disposable task = Mono.
                from(Flux.range(0, 1))
                .flatMap(__ -> bank.close(selectedAccountId.get()))
                .doOnError(Throwable::printStackTrace)
                .doOnTerminate(() -> {
                    System.out.println("successfully done !");
                    System.out.println("Account " + selectedAccountId + " was closed.");
                })
                .subscribe();

        while(!task.isDisposed()){try {sleep(50l);} catch (InterruptedException e) {}}
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

    private Option<String> requestYesOrCancel(){

        Option<String> response = Option.none();
        Scanner sc= new Scanner(System.in);
        String input;

        while (response.isEmpty()) {
            input = sc.nextLine().trim();
            if(input.equalsIgnoreCase("CANCEL")){
                return response; // which means : to abort ongoing action
            }
            if(input.equalsIgnoreCase("YES")){
                response = Option.some("YES");
                return response;
            }
            // invalid answer
            System.out.print("Please type 'cancel' or 'yes': ");
        }
        return response;
    }

    private void setAccount(){
        System.out.println("Set account");
    }
}