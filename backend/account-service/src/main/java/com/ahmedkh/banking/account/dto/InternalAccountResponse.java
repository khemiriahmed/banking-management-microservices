package com.ahmedkh.banking.account.dto;

import com.ahmedkh.banking.account.entity.AccountStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalAccountResponse(
        UUID accountId,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance
) {
}
