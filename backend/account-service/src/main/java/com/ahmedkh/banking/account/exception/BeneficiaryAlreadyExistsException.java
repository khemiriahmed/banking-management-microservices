package com.ahmedkh.banking.account.exception;

public class BeneficiaryAlreadyExistsException extends RuntimeException {

    public BeneficiaryAlreadyExistsException() {
        super("Beneficiary already exists for this client");
    }
}
