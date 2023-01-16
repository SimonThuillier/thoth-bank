package com.bts.thoth.bank;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.bts.thoth.bank.BankCliRunner;
import reactor.core.Disposable;

import java.util.concurrent.CountDownLatch;

import static io.vavr.API.println;
import static java.lang.Thread.sleep;

@SpringBootApplication
public class BankCliApplication {

    public static void main(String[] args) throws InterruptedException {

        ApplicationContext context = new AnnotationConfigApplicationContext(
                "com.bts.thoth.bank", "com.bts.thoth.bank.config", "com.bts.thoth.bank.repository");

        Bank bank = context.getBean(Bank.class);

        println("Thoth bank backend initializing, please wait...");
        CountDownLatch myLatch = new CountDownLatch(1);

        bank.init().doOnTerminate(() -> {
            LoggerFactory.getLogger("Bank").info("Thoth bank backend successfully initialized");
            println("Initialization successful !");
            myLatch.countDown();
        }).subscribe();

        myLatch.await();
        context.getBean(BankCliRunner.class).run();
    }
}