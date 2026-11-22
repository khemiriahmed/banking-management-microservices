package com.ahmedkh.banking.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBeneficiaryRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^FR76 \\*{4} \\*{4} \\*{4} \\d{4}$")
        String destinationAccountReference
) {
}
