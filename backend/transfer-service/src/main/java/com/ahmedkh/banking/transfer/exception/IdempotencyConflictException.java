package com.ahmedkh.banking.transfer.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key already used with a different request");
    }
}
