package com.bankcore360.exception;

public class InsufficientLedgerFundsException extends RuntimeException {
    public InsufficientLedgerFundsException(String message) {
        super(message);
    }
}
