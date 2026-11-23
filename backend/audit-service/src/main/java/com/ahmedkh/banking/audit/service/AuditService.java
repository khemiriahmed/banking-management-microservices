package com.ahmedkh.banking.audit.service;

import com.ahmedkh.banking.audit.dto.AuditEventResponse;
import com.ahmedkh.banking.audit.dto.AuditPageResponse;
import com.ahmedkh.banking.audit.entity.AuditEvent;
import com.ahmedkh.banking.audit.event.BankingEvent;
import com.ahmedkh.banking.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final int MAX_SIZE = 50;

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void consume(BankingEvent event) {
        if (event == null || event.eventId() == null || event.eventType() == null) {
            throw new IllegalArgumentException("Invalid event");
        }
        if (repository.existsByEventId(event.eventId())) {
            logger.info("Duplicate audit event ignored: {}", event.eventId());
            return;
        }
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        UUID actorId = parseUuid(payload.get("ownerUserId"));
        String action = "TransferCompleted".equals(event.eventType()) ? "TRANSFER_COMPLETED" : "TRANSFER_FAILED";
        String targetId = string(payload.get("transferId"));
        Map<String, Object> metadata = new LinkedHashMap<>();
        copy(metadata, payload, "status");
        copy(metadata, payload, "currency");
        copy(metadata, payload, "amount");
        copy(metadata, payload, "failureReason");
        AuditEvent auditEvent = new AuditEvent(
                UUID.randomUUID(), event.eventId(), event.eventType(), actorId, action, targetId,
                event.occurredAt() == null ? Instant.now() : event.occurredAt(), toJson(metadata), Instant.now()
        );
        try {
            repository.save(auditEvent);
        } catch (DataIntegrityViolationException e) {
            logger.info("Duplicate audit event raced: {}", event.eventId());
        }
    }

    @Transactional(readOnly = true)
    public AuditPageResponse list(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        Page<AuditEvent> result = repository.findAllByOrderByOccurredAtDesc(PageRequest.of(safePage, safeSize));
        return new AuditPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.isLast()
        );
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getEventId(), e.getEventType(), e.getActorId(), e.getAction(),
                e.getTargetId(), e.getOccurredAt(), e.getMetadata());
    }

    private void copy(Map<String, Object> target, Map<String, Object> payload, String key) {
        if (payload.containsKey(key) && payload.get(key) != null) {
            target.put(key, payload.get(key));
        }
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private UUID parseUuid(Object value) {
        if (value == null) return null;
        try { return UUID.fromString(value.toString()); } catch (Exception e) { return null; }
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
