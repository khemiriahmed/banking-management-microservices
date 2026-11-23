package com.ahmedkh.banking.audit.service;

import com.ahmedkh.banking.audit.config.RabbitTopologyConfig;
import com.ahmedkh.banking.audit.event.BankingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @RabbitListener(queues = RabbitTopologyConfig.AUDIT_QUEUE)
    public void onEvent(BankingEvent event) {
        try {
            auditService.consume(event);
        } catch (RuntimeException e) {
            logger.error("Failed to consume audit event {}: {}", event == null ? null : event.eventId(), e.getMessage());
            throw e;
        }
    }
}
