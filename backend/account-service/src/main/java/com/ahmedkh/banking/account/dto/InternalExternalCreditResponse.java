package com.ahmedkh.banking.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalExternalCreditResponse(
        UUID externalAccountId,
        String destinationReference,
        BigDecimal balance,
        boolean alreadyProcessed
) {
}
