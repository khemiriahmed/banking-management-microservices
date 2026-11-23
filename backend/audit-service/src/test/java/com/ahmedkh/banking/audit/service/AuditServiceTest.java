package com.ahmedkh.banking.audit.service;

import com.ahmedkh.banking.audit.entity.AuditEvent;
import com.ahmedkh.banking.audit.event.BankingEvent;
import com.ahmedkh.banking.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    private AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(repository, new ObjectMapper());
    }

    @Test
    void duplicateEventIsIgnored() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(true);

        service.consume(new BankingEvent(eventId, "TransferFailed", Instant.now(), Map.of("ownerUserId", UUID.randomUUID().toString())));

        verify(repository, never()).save(any());
    }

    @Test
    void failedEventCreatesAuditRecord() {
        UUID eventId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);

        service.consume(new BankingEvent(eventId, "TransferFailed", Instant.now(), Map.of(
                "ownerUserId", owner.toString(), "transferId", transferId.toString(), "amount", new BigDecimal("25.00"), "currency", "EUR", "status", "FAILED"
        )));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("TRANSFER_FAILED");
        assertThat(captor.getValue().getTargetId()).isEqualTo(transferId.toString());
    }
}
