package com.ahmedkh.banking.account.exception;

public class BeneficiaryNotFoundException extends RuntimeException {

    public BeneficiaryNotFoundException() {
        super("Beneficiary not found");
    }
}
