package com.ahmedkh.banking.account.controller;

import com.ahmedkh.banking.account.dto.BeneficiaryResponse;
import com.ahmedkh.banking.account.dto.CreateBeneficiaryRequest;
import com.ahmedkh.banking.account.dto.UpdateBeneficiaryRequest;
import com.ahmedkh.banking.account.security.AuthenticatedUser;
import com.ahmedkh.banking.account.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService service;

    public BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @GetMapping
    public List<BeneficiaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listForOwner(user.getAuthUserId());
    }

    @GetMapping("/{id}")
    public BeneficiaryResponse get(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.getForOwner(id, user.getAuthUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse create(
            @Valid @RequestBody CreateBeneficiaryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.create(request, user.getAuthUserId());
    }

    @PutMapping("/{id}")
    public BeneficiaryResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBeneficiaryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.update(id, request, user.getAuthUserId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        service.delete(id, user.getAuthUserId());
    }
}
