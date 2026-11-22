package com.ahmedkh.banking.account.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException() {
        super("Amount currency does not match account currency");
    }
}
