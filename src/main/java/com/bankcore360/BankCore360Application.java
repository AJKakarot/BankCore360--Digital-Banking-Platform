package com.bankcore360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the BankCore 360 Educational Digital Banking Application.
 */
@SpringBootApplication
@EnableScheduling
public class BankCore360Application {

    public static void main(String[] args) {
        SpringApplication.run(BankCore360Application.class, args);
    }
}
