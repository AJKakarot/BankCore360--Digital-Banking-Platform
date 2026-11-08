package com.bankcore360.exception;

public class LedgerTransactionNotFoundException extends RuntimeException {
    public LedgerTransactionNotFoundException(String message) {
        super(message);
    }
}
