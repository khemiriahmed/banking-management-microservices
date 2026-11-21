package com.ahmedkh.banking.user.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        UUID authUserId,
        String firstName,
        String lastName,
        String phone,
        Instant createdAt
) {
}
