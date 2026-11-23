package com.ahmedkh.banking.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionEntryResponse(
        UUID id,
        UUID accountId,
        String destinationReference,
        EntrySideDto side,
        BigDecimal amount,
        String currency
) {
}
