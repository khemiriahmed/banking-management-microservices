package com.ahmedkh.banking.notification.service;

import com.ahmedkh.banking.notification.entity.Notification;
import com.ahmedkh.banking.notification.event.BankingEvent;
import com.ahmedkh.banking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    @Test
    void duplicateEventIsIgnored() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(true);

        service.consume(new BankingEvent(eventId, "TransferCompleted", Instant.now(), Map.of("ownerUserId", UUID.randomUUID().toString())));

        verify(repository, never()).save(any());
    }

    @Test
    void completedEventCreatesNotification() {
        UUID eventId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);

        service.consume(new BankingEvent(eventId, "TransferCompleted", Instant.now(), Map.of(
                "ownerUserId", owner.toString(), "amount", new BigDecimal("25.00"), "currency", "EUR", "status", "COMPLETED"
        )));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOwnerUserId()).isEqualTo(owner);
        assertThat(captor.getValue().getEventType()).isEqualTo("TransferCompleted");
    }
}
