package com.ahmedkh.banking.transfer.exception;

public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException() {
        super("Transfer not found");
    }
}
