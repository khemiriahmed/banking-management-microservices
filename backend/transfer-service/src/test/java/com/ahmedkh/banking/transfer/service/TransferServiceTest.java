package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.CreateTransferRequest;
import com.ahmedkh.banking.transfer.dto.TransferResponse;
import com.ahmedkh.banking.transfer.entity.CompensationStatus;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import com.ahmedkh.banking.transfer.entity.TransferStatusHistory;
import com.ahmedkh.banking.transfer.exception.IdempotencyConflictException;
import com.ahmedkh.banking.transfer.repository.TransferRepository;
import com.ahmedkh.banking.transfer.repository.TransferStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final String KEY = "checkout-1";

    @Mock
    private TransferRepository repository;

    @Mock
    private TransferStatusHistoryRepository historyRepository;

    @Mock
    private TransferOrchestrator orchestrator;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(
                repository,
                historyRepository,
                orchestrator,
                new TransferResponseMapper(historyRepository)
        );
    }

    @Test
    void createsRequestedTransferAndRunsOrchestrator() {
        CreateTransferRequest request = request();
        UUID owner = UUID.randomUUID();
        AtomicReference<Transfer> saved = new AtomicReference<>();
        when(repository.findByOwnerUserIdAndIdempotencyKey(owner, KEY)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            saved.set(transfer);
            return transfer;
        });
        when(repository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.of(saved.get()));

        TransferResponse response = service.create(request, KEY, owner, AUTHORIZATION);

        ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);
        verify(repository).saveAndFlush(captor.capture());
        Transfer transfer = captor.getValue();
        assertThat(response.id()).isEqualTo(transfer.getId());
        assertThat(response.status()).isEqualTo(TransferStatus.REQUESTED);
        assertThat(transfer.getOwnerUserId()).isEqualTo(owner);
        assertThat(transfer.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(transfer.getRequestFingerprint()).isEqualTo(fingerprint(request, owner));
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.NONE);
        verify(historyRepository).save(argThat(entry ->
                entry.getToStatus() == TransferStatus.REQUESTED
                        && entry.getTransferId().equals(transfer.getId())
        ));
        verify(orchestrator).execute(transfer, AUTHORIZATION);
    }

    @Test
    void sameKeyAndPayloadReplaysExistingTransfer() {
        UUID owner = UUID.randomUUID();
        CreateTransferRequest request = request();
        Transfer existing = transfer(owner, fingerprint(request, owner));
        when(repository.findByOwnerUserIdAndIdempotencyKey(owner, KEY)).thenReturn(Optional.of(existing));

        TransferResponse response = service.create(request, KEY, owner, AUTHORIZATION);

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(orchestrator, never()).execute(any(), anyString());
        verify(repository, never()).saveAndFlush(any());
        verify(historyRepository, never()).save(any(TransferStatusHistory.class));
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejected() {
        UUID owner = UUID.randomUUID();
        Transfer existing = transfer(owner, "different-fingerprint");
        when(repository.findByOwnerUserIdAndIdempotencyKey(owner, KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(request(), KEY, owner, AUTHORIZATION))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(orchestrator, never()).execute(any(), anyString());
    }

    @Test
    void concurrentDuplicateReplaysExistingTransfer() {
        UUID owner = UUID.randomUUID();
        CreateTransferRequest request = request();
        Transfer existing = transfer(owner, fingerprint(request, owner));
        when(repository.findByOwnerUserIdAndIdempotencyKey(owner, KEY))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        TransferResponse response = service.create(request, KEY, owner, AUTHORIZATION);

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(orchestrator, never()).execute(any(), anyString());
    }

    private CreateTransferRequest request() {
        return new CreateTransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                "EUR",
                "invoice-42"
        );
    }

    private Transfer transfer(UUID owner, String fingerprint) {
        return new Transfer(
                UUID.randomUUID(),
                owner,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PENDING",
                new BigDecimal("50.00"),
                "EUR",
                "invoice-42",
                TransferStatus.COMPLETED,
                KEY,
                fingerprint,
                Instant.now()
        );
    }

    private String fingerprint(CreateTransferRequest request, UUID owner) {
        String canonical = String.join("|",
                owner.toString(),
                request.sourceAccountId().toString(),
                request.beneficiaryId().toString(),
                request.amount().setScale(2).toPlainString(),
                request.currency(),
                request.reason() == null ? "" : request.reason()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
