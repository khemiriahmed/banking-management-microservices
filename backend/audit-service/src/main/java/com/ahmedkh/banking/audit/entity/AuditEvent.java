package com.ahmedkh.banking.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, UUID eventId, String eventType, UUID actorId, String action, String targetId,
                      Instant occurredAt, String metadata, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.actorId = actorId;
        this.action = action;
        this.targetId = targetId;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getTargetId() { return targetId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
