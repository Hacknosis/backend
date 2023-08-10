package com.hacknosis.backend.exceptions;

public class AccountInfoConflictException extends RuntimeException {
    public AccountInfoConflictException(String message) {
        super(message);
    }
}
