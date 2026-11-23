package com.ahmedkh.banking.transfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalAccountResponse(
        UUID accountId,
        String status,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance
) {
}
