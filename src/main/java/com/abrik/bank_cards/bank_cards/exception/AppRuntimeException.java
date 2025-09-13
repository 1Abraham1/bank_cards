package com.abrik.bank_cards.bank_cards.exception;

public abstract class AppRuntimeException extends RuntimeException {
    public AppRuntimeException(String message) {
        super(message);
    }
}
