package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.config.RabbitConfig;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import com.ahmedkh.banking.transfer.event.TransferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TransferEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TransferEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TransferEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTerminalEvent(Transfer transfer) {
        if (transfer.getStatus() != TransferStatus.COMPLETED && transfer.getStatus() != TransferStatus.FAILED) {
            return;
        }
        String eventType = transfer.getStatus() == TransferStatus.COMPLETED ? "TransferCompleted" : "TransferFailed";
        String routingKey = transfer.getStatus() == TransferStatus.COMPLETED ? "banking.transfer.completed" : "banking.transfer.failed";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferId", transfer.getId().toString());
        payload.put("ownerUserId", transfer.getOwnerUserId().toString());
        payload.put("sourceAccountId", transfer.getSourceAccountId().toString());
        payload.put("beneficiaryId", transfer.getBeneficiaryId().toString());
        payload.put("amount", transfer.getAmount());
        payload.put("currency", transfer.getCurrency());
        payload.put("status", transfer.getStatus().name());
        if (transfer.getFailureReason() != null) {
            payload.put("failureReason", transfer.getFailureReason());
        }
        TransferEvent event = new TransferEvent(UUID.randomUUID(), eventType, Instant.now(), payload);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        } catch (RuntimeException e) {
            logger.warn("Failed to publish {} for transfer {}: {}", eventType, transfer.getId(), e.getMessage());
        }
    }
}
