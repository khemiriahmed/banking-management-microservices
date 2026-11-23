package com.ahmedkh.banking.transfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalTransactionEntryRequest(
        UUID accountId,
        String destinationReference,
        String side,
        BigDecimal amount,
        String currency
) {
}
