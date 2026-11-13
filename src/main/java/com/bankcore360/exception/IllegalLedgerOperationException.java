package com.bankcore360.exception;

public class IllegalLedgerOperationException extends RuntimeException {
    public IllegalLedgerOperationException(String message) {
        super(message);
    }
}

