package com.ahmedkh.banking.audit.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BankingEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
