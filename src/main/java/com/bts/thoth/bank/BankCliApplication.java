package com.bts.thoth.bank;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankCliApplication
        implements CommandLineRunner {

    private static Logger LOG = LoggerFactory
            .getLogger(BankCliApplication.class);

    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        //CommandLineRunner.run(BankCliApplication.class, args);
        LOG.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) {
        LOG.info("EXECUTING : command line runner");

        for (int i = 0; i < args.length; ++i) {
            LOG.info("args[{" + i +  "}]: {" + args[i] + "}");
        }
    }
}