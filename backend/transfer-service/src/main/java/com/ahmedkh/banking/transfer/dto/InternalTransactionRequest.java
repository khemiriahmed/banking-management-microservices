package com.ahmedkh.banking.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InternalTransactionRequest(
        String externalReference,
        UUID transferId,
        UUID ownerUserId,
        String type,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        List<InternalTransactionEntryRequest> entries
) {
}
