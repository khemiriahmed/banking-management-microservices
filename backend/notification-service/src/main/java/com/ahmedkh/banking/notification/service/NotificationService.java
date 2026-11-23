package com.ahmedkh.banking.notification.service;

import com.ahmedkh.banking.notification.dto.NotificationPageResponse;
import com.ahmedkh.banking.notification.dto.NotificationResponse;
import com.ahmedkh.banking.notification.entity.Notification;
import com.ahmedkh.banking.notification.event.BankingEvent;
import com.ahmedkh.banking.notification.exception.NotificationNotFoundException;
import com.ahmedkh.banking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_SIZE = 50;

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void consume(BankingEvent event) {
        if (event == null || event.eventId() == null || event.eventType() == null) {
            throw new IllegalArgumentException("Invalid event");
        }
        if (repository.existsByEventId(event.eventId())) {
            logger.info("Duplicate notification event ignored: {}", event.eventId());
            return;
        }
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        UUID ownerUserId = parseUuid(payload.get("ownerUserId"));
        if (ownerUserId == null) {
            logger.warn("Notification event without ownerUserId ignored: {}", event.eventId());
            return;
        }
        String title = "TransferCompleted".equals(event.eventType()) ? "Virement confirme" : "Virement echoue";
        String status = string(payload.get("status"), "UNKNOWN");
        String amount = string(payload.get("amount"), "");
        String currency = string(payload.get("currency"), "");
        String message = "TransferCompleted".equals(event.eventType())
                ? "Votre virement de " + amount + " " + currency + " est confirme."
                : "Votre virement de " + amount + " " + currency + " a echoue (" + status + ").";
        Notification notification = new Notification(
                UUID.randomUUID(), event.eventId(), event.eventType(), ownerUserId,
                title, message, event.occurredAt() == null ? Instant.now() : event.occurredAt(), Instant.now()
        );
        try {
            repository.save(notification);
        } catch (DataIntegrityViolationException e) {
            logger.info("Duplicate notification event raced: {}", event.eventId());
        }
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(UUID ownerUserId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        Page<Notification> result = repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, PageRequest.of(safePage, safeSize));
        return new NotificationPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.isLast()
        );
    }

    @Transactional
    public NotificationResponse markRead(UUID id, UUID ownerUserId) {
        Notification notification = repository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.setRead(true);
        return toResponse(repository.save(notification));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventType(), n.getTitle(), n.getMessage(),
                n.isRead(), n.getOccurredAt(), n.getCreatedAt());
    }

    private UUID parseUuid(Object value) {
        if (value == null) return null;
        try { return UUID.fromString(value.toString()); } catch (Exception e) { return null; }
    }

    private String string(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
