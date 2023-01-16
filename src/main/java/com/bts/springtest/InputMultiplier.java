package com.bts.springtest;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.CommandLineRunner;

import java.util.Scanner;

public class InputMultiplier implements CommandLineRunner, Runnable {

    private final Scanner scanner;

    public InputMultiplier() {
        scanner = new Scanner(System.in);
    }

    @Override
    public void run(String... args) throws Exception {
        while(true){
            System.out.print("Enter a positive integer: ");
            int input = scanner.nextInt();
            if(input == 0){return;}
            System.out.println("Twice the entered number is: " + 2 * input);
        }
    }

    @PreDestroy
    public void destroy() {
        scanner.close();
    }

    @Override
    public void run() {
        try {
            this.run("nothing");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
