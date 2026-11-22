package com.ahmedkh.banking.auth.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email already used: " + email);
    }
}
