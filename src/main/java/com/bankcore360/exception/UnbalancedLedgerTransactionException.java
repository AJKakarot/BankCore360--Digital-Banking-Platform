package com.bankcore360.exception;

public class UnbalancedLedgerTransactionException extends RuntimeException {
    public UnbalancedLedgerTransactionException(String message) {
        super(message);
    }
}

