package com.ahmedkh.banking.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID eventId,
        String eventType,
        UUID actorId,
        String action,
        String targetId,
        Instant occurredAt,
        String metadata
) {
}
