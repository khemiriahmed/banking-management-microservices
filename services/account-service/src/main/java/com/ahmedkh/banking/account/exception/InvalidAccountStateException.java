package com.ahmedkh.banking.account.exception;

public class InvalidAccountStateException extends RuntimeException {

    public InvalidAccountStateException() {
        super("Account status does not allow this operation");
    }
}
