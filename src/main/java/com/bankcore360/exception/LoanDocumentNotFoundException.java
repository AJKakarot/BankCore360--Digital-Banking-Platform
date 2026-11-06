package com.bankcore360.exception;

public class LoanDocumentNotFoundException extends RuntimeException {
    public LoanDocumentNotFoundException(String message) {
        super(message);
    }
}

