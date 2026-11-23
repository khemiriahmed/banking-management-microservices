package com.ahmedkh.banking.notification.service;

import com.ahmedkh.banking.notification.config.RabbitTopologyConfig;
import com.ahmedkh.banking.notification.event.BankingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitTopologyConfig.NOTIFICATION_QUEUE)
    public void onEvent(BankingEvent event) {
        try {
            notificationService.consume(event);
        } catch (RuntimeException e) {
            logger.error("Failed to consume notification event {}: {}", event == null ? null : event.eventId(), e.getMessage());
            throw e;
        }
    }
}
