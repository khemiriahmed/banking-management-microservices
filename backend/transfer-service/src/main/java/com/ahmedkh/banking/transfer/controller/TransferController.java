package com.ahmedkh.banking.transfer.controller;

import com.ahmedkh.banking.transfer.dto.CreateTransferRequest;
import com.ahmedkh.banking.transfer.dto.StatusChangeResponse;
import com.ahmedkh.banking.transfer.dto.TransferPageResponse;
import com.ahmedkh.banking.transfer.dto.TransferResponse;
import com.ahmedkh.banking.transfer.security.AuthenticatedUser;
import com.ahmedkh.banking.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must contain 1 to 128 characters");
        }
        TransferResponse response = service.create(
                request,
                idempotencyKey,
                user.getAuthUserId(),
                authorization
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public TransferPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.list(user.getAuthUserId(), page, size);
    }

    @GetMapping("/{id}")
    public TransferResponse get(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.get(id, user.getAuthUserId());
    }

    @GetMapping("/{id}/history")
    public List<StatusChangeResponse> history(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.history(id, user.getAuthUserId());
    }
}
