package com.ahmedkh.banking.transaction.service;

import com.ahmedkh.banking.transaction.dto.CreateTransactionEntryRequest;
import com.ahmedkh.banking.transaction.dto.CreateTransactionRequest;
import com.ahmedkh.banking.transaction.dto.EntrySideDto;
import com.ahmedkh.banking.transaction.dto.TransactionPageResponse;
import com.ahmedkh.banking.transaction.dto.TransactionTypeDto;
import com.ahmedkh.banking.transaction.entity.TransactionRecord;
import com.ahmedkh.banking.transaction.entity.TransactionStatus;
import com.ahmedkh.banking.transaction.entity.TransactionType;
import com.ahmedkh.banking.transaction.exception.TransactionNotFoundException;
import com.ahmedkh.banking.transaction.repository.TransactionRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRecordRepository repository;

    @Test
    void recordCreatesConfirmedTransactionWithEntries() {
        TransactionService service = new TransactionService(repository);
        when(repository.findByExternalReference("transfer-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(TransactionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(request("transfer-1", BigDecimal.TEN, "EUR", "EUR")).transaction();

        ArgumentCaptor<TransactionRecord> captor = ArgumentCaptor.forClass(TransactionRecord.class);
        verify(repository).saveAndFlush(captor.capture());
        TransactionRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(saved.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.entries()).hasSize(2);
        assertThat(response.entries().get(0).side()).isEqualTo(EntrySideDto.DEBIT);
        assertThat(response.entries().get(1).side()).isEqualTo(EntrySideDto.CREDIT);
        assertThat(response.externalReference()).isEqualTo("transfer-1");
    }

    @Test
    void recordReturnsExistingWhenExternalReferenceAlreadyProcessed() {
        TransactionService service = new TransactionService(repository);
        TransactionRecord existing = new TransactionRecord(
                UUID.randomUUID(), "transfer-1", UUID.randomUUID(), UUID.randomUUID(),
                TransactionType.TRANSFER, TransactionStatus.CONFIRMED,
                BigDecimal.TEN, "EUR", Instant.now(), Instant.now()
        );
        when(repository.findByExternalReference("transfer-1")).thenReturn(Optional.of(existing));

        var response = service.record(request("transfer-1", BigDecimal.TEN, "EUR", "EUR"));

        assertThat(response.alreadyProcessed()).isTrue();
        assertThat(response.transaction().id()).isEqualTo(existing.getId());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void recordRejectsMismatchedEntryCurrency() {
        TransactionService service = new TransactionService(repository);
        when(repository.findByExternalReference("transfer-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(request("transfer-1", BigDecimal.TEN, "EUR", "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void findForOwnerRejectsAnotherOwner() {
        TransactionService service = new TransactionService(repository);
        when(repository.findByIdAndOwnerUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findForOwner(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void listValidatesPaginationSortingAndDateRange() {
        TransactionService service = new TransactionService(repository);
        UUID owner = UUID.randomUUID();
        when(repository.findByOwnerUserIdAndOccurredAtBetween(eq(owner), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        TransactionPageResponse page = service.listForOwner(
                owner, null, Instant.EPOCH, Instant.now(), 0, 10, "amount", "asc"
        );

        assertThat(page.content()).isEmpty();
        assertThat(page.size()).isEqualTo(10);
        assertThatThrownBy(() -> service.listForOwner(
                owner, null, null, null, 0, 51, "occurredAt", "desc"
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listForOwner(
                owner, null, null, null, 0, 10, "ownerUserId", "desc"
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listForOwner(
                owner, null, Instant.now(), Instant.EPOCH, 0, 10, "occurredAt", "desc"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private CreateTransactionRequest request(
            String externalReference,
            BigDecimal amount,
            String currency,
            String entryCurrency
    ) {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        return new CreateTransactionRequest(
                externalReference,
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransactionTypeDto.TRANSFER,
                amount,
                currency,
                Instant.now(),
                List.of(
                        new CreateTransactionEntryRequest(source, null, EntrySideDto.DEBIT, amount, entryCurrency),
                        new CreateTransactionEntryRequest(
                                null, destination.toString(), EntrySideDto.CREDIT, amount, entryCurrency
                        )
                )
        );
    }
}
