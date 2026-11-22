package com.ahmedkh.banking.account.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Insufficient available balance");
    }
}
