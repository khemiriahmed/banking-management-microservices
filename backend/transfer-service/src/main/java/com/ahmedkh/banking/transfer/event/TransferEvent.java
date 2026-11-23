package com.ahmedkh.banking.transfer.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TransferEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
