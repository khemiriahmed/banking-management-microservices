package com.ahmedkh.banking.transaction.controller;

import com.ahmedkh.banking.transaction.dto.TransactionPageResponse;
import com.ahmedkh.banking.transaction.dto.TransactionResponse;
import com.ahmedkh.banking.transaction.security.AuthenticatedUser;
import com.ahmedkh.banking.transaction.service.TransactionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public TransactionPageResponse list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return service.listForOwner(
                user.getAuthUserId(), accountId, from, to, page, size, sortBy, direction
        );
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        return service.findForOwner(id, user.getAuthUserId());
    }
}
