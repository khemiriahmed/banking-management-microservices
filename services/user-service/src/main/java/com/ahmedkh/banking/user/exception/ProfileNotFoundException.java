package com.ahmedkh.banking.user.exception;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("Customer profile not found");
    }
}
