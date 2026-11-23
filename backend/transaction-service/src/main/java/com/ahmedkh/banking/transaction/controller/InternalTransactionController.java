package com.ahmedkh.banking.transaction.controller;

import com.ahmedkh.banking.transaction.dto.CreateTransactionRequest;
import com.ahmedkh.banking.transaction.dto.CreateTransactionResponse;
import com.ahmedkh.banking.transaction.security.InternalTokenFilter;
import com.ahmedkh.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/transactions")
public class InternalTransactionController {

    private final TransactionService service;

    public InternalTransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreateTransactionResponse> create(
            @RequestHeader(InternalTokenFilter.TOKEN_HEADER) String internalToken,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        CreateTransactionResponse response = service.record(request);
        HttpStatus status = response.alreadyProcessed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
