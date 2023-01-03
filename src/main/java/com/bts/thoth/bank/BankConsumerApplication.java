package com.bts.thoth.bank;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.bts.thoth.bank.core.*;

@SpringBootApplication
public class BankConsumerApplication {


	public static void main(String[] args) {
		BankAsynchronousProjector asyncProjector = new BankAsynchronousProjector();
		asyncProjector.run();
	}
}
