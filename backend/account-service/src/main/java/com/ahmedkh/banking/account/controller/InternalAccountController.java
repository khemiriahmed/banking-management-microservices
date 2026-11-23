package com.ahmedkh.banking.account.controller;

import com.ahmedkh.banking.account.dto.InternalAccountResponse;
import com.ahmedkh.banking.account.dto.InternalAmountRequest;
import com.ahmedkh.banking.account.dto.InternalExternalCreditRequest;
import com.ahmedkh.banking.account.dto.InternalExternalCreditResponse;
import com.ahmedkh.banking.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/accounts")
public class InternalAccountController {

    private final AccountService service;

    public InternalAccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/{id}/reserve")
    public InternalAccountResponse reserve(
            @PathVariable UUID id,
            @Valid @RequestBody InternalAmountRequest request
    ) {
        return service.reserve(id, request.amount(), request.currency());
    }

    @PostMapping("/{id}/debit")
    public InternalAccountResponse debit(
            @PathVariable UUID id,
            @Valid @RequestBody InternalAmountRequest request
    ) {
        return service.debit(id, request.amount(), request.currency());
    }

    @PostMapping("/{id}/credit")
    public InternalAccountResponse credit(
            @PathVariable UUID id,
            @Valid @RequestBody InternalAmountRequest request
    ) {
        return service.credit(id, request.amount(), request.currency());
    }

    @PostMapping("/{id}/release")
    public InternalAccountResponse release(
            @PathVariable UUID id,
            @Valid @RequestBody InternalAmountRequest request
    ) {
        return service.release(id, request.amount(), request.currency());
    }

    @PostMapping("/external-credit")
    public InternalExternalCreditResponse creditExternal(
            @Valid @RequestBody InternalExternalCreditRequest request
    ) {
        return service.creditExternal(request);
    }
}
