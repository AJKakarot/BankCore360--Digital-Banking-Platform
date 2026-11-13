package com.bankcore360.exception;

public class LedgerAccountNotFoundException extends RuntimeException {
    public LedgerAccountNotFoundException(String message) {
        super(message);
    }
}

