package com.bankcore360.exception;

public class InvalidKycTransitionException extends RuntimeException {
    public InvalidKycTransitionException(String message) {
        super(message);
    }
}
