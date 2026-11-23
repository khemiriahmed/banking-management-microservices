package com.ahmedkh.banking.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventType,
        String title,
        String message,
        boolean read,
        Instant occurredAt,
        Instant createdAt
) {
}
