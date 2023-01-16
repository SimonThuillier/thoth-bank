package com.bts.thoth.bank;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class BankConsumerApplication {


	public static void main(String[] args) {

		ApplicationContext context = new AnnotationConfigApplicationContext(
				"com.bts.thoth.bank", "com.bts.thoth.bank.config");

		BankAsynchronousProjector asyncProjector = new BankAsynchronousProjector(context);
		asyncProjector.run();
	}
}
