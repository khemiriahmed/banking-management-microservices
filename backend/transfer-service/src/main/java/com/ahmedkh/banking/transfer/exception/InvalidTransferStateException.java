package com.ahmedkh.banking.transfer.exception;

public class InvalidTransferStateException extends RuntimeException {

    public InvalidTransferStateException(String message) {
        super(message);
    }
}
