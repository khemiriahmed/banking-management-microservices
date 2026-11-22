package com.ahmedkh.banking.account.dto;

import com.ahmedkh.banking.account.entity.AccountStatus;
import com.ahmedkh.banking.account.entity.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID ownerUserId,
        String accountNumberMasked,
        AccountType accountType,
        String currency,
        AccountStatus status,
        BigDecimal balance,
        Instant createdAt
) {
}
