package com.ahmedkh.banking.account.controller;

import com.ahmedkh.banking.account.dto.AccountResponse;
import com.ahmedkh.banking.account.security.AuthenticatedUser;
import com.ahmedkh.banking.account.service.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listForOwner(user.getAuthUserId());
    }

    @GetMapping("/{id}")
    public AccountResponse get(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.getForOwner(id, user.getAuthUserId());
    }
}
