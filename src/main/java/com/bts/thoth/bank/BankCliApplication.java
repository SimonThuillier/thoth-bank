package com.bts.thoth.bank;

import io.vavr.control.Option;

import static io.vavr.API.*;
import static io.vavr.Predicates.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static io.vavr.API.run;

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

    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
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

    public void createNewAccount(){
        System.out.println("1");
    }

    private void runPrimaryMenu(){

        Integer result = 0;
        System.out.println(menuText);
        System.out.print("Type a choice and enter to validate: ");
        Scanner sc= new Scanner(System.in);
        Option<String> input = Option.of(sc.nextLine().trim()); //reads string.

        //System.out.println("Invalid input. Please enter a valid action number.");

        while (result <= 0){
            input = Option.of(sc.nextLine().trim()); //reads string.

            result = Match(input).of(
                    Case($(instanceOf(Integer.class)), Integer::valueOf),
                    Case($(), o -> -1)
            );
            result = Match(Option.of(result)).of(
                    Case($(isIn(1, 2 , 3)), o -> o),
                    Case($(), o -> -1)
            );

            if(result == -1){
                System.out.println("Invalid input. Please enter a valid action number.");
            }


        }



//        if(result)
//
//        result = Match(Option.of(result)).of(
//                Case($(is(1)), o -> io.vavr.API.run(() -> {
//                    System.out.println("run create");
//                })),
//                Case($(is(2)), o -> io.vavr.API.run(() -> {
//                    System.out.println("run set account");
//                })),
//                Case($(is(3)), o -> io.vavr.API.run(() -> {
//                    System.out.println("run exit app");
//                })),
//                Case($(), d -> -1)
//        );





    }
}