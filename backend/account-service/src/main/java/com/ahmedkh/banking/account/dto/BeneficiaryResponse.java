package com.ahmedkh.banking.account.dto;

import com.ahmedkh.banking.account.entity.BeneficiaryStatus;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        UUID ownerUserId,
        String displayName,
        String destinationAccountReference,
        BeneficiaryStatus status,
        Instant createdAt
) {
}
