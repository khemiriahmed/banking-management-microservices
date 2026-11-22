package com.ahmedkh.banking.account.dto;

import com.ahmedkh.banking.account.entity.BeneficiaryStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBeneficiaryRequest(
        @Size(max = 100) String displayName,
        @Size(max = 32)
        @Pattern(regexp = "^FR76 \\*{4} \\*{4} \\*{4} \\d{4}$")
        String destinationAccountReference,
        BeneficiaryStatus status
) {
}
