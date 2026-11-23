package com.ahmedkh.banking.transfer.dto;

import java.math.BigDecimal;

public record InternalExternalCreditRequest(
        String destinationReference,
        BigDecimal amount,
        String currency,
        String operationId
) {
}
