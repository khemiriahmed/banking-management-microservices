package com.ahmedkh.banking.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InternalExternalCreditRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^FR76 \\*{4} \\*{4} \\*{4} \\d{4}$")
        String destinationReference,

        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,

        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,

        @NotBlank @Size(max = 128) String operationId
) {
}
