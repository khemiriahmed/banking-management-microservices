package com.ahmedkh.banking.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotBlank @Size(max = 128) String externalReference,
        @NotNull UUID transferId,
        @NotNull UUID ownerUserId,
        @NotNull TransactionTypeDto type,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull Instant occurredAt,
        @NotEmpty @Valid List<CreateTransactionEntryRequest> entries
) {
}
