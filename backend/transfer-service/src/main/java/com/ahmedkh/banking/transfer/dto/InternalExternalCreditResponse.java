package com.ahmedkh.banking.transfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalExternalCreditResponse(
        UUID externalAccountId,
        String destinationReference,
        BigDecimal balance,
        boolean alreadyProcessed
) {
}
