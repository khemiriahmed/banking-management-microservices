package com.ahmedkh.banking.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String externalReference,
        UUID transferId,
        UUID ownerUserId,
        TransactionTypeDto type,
        String status,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        Instant createdAt,
        List<TransactionEntryResponse> entries
) {
}
