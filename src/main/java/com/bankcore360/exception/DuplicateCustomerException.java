package com.bankcore360.exception;

/**
 * Exception thrown when attempting to register a customer with an email
 * that already exists in the system.
 */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String message) {
        super(message);
    }
}
