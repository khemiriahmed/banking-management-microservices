package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.AccountView;
import com.ahmedkh.banking.transfer.dto.BeneficiaryView;
import com.ahmedkh.banking.transfer.entity.CompensationStatus;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import com.ahmedkh.banking.transfer.entity.TransferStatusHistory;
import com.ahmedkh.banking.transfer.exception.TransferBusinessException;
import com.ahmedkh.banking.transfer.repository.TransferRepository;
import com.ahmedkh.banking.transfer.repository.TransferStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferOrchestratorTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final String DESTINATION_REFERENCE = "FR76 **** **** **** 1234";

    @Mock
    private AccountServiceClient client;

    @Mock
    private TransactionServiceClient transactionClient;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferStatusHistoryRepository historyRepository;

    private TransferOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new TransferOrchestrator(
                client, transactionClient, transferRepository, historyRepository
        );
    }

    @Test
    void successfulTransferCompletesWithoutCompensation() {
        Transfer transfer = newTransfer();
        stubValidSourceAndBeneficiary(transfer);

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.NOT_REQUIRED);
        assertThat(transfer.getDestinationReference()).isEqualTo(DESTINATION_REFERENCE);
        verify(client).reserve(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        verify(client).debit(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        verify(client).creditDestination(
                eq(DESTINATION_REFERENCE),
                eq(transfer.getAmount()),
                eq(transfer.getCurrency()),
                eq(transfer.getId() + ":credit")
        );
        verify(client, never()).release(any(), any(), any());
        verify(client, never()).creditSource(any(), any(), any());
        verify(transactionClient).recordCompletedTransfer(transfer);
        assertHistory(TransferStatus.VALIDATING, TransferStatus.PROCESSING, TransferStatus.COMPLETED);
    }

    @Test
    void insufficientBalanceFailsBeforeAnyMutation() {
        Transfer transfer = newTransfer();
        stubValidSourceAndBeneficiary(transfer);
        doThrow(new TransferBusinessException("Insufficient available balance"))
                .when(client).reserve(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.NOT_REQUIRED);
        assertThat(transfer.getFailureReason()).contains("Insufficient available balance");
        verify(client, never()).debit(any(), any(), any());
        verify(client, never()).creditDestination(anyString(), any(), anyString(), anyString());
        assertHistory(TransferStatus.VALIDATING, TransferStatus.PROCESSING, TransferStatus.FAILED);
    }

    @Test
    void invalidBeneficiaryFailsBeforeReservation() {
        Transfer transfer = newTransfer();
        when(client.getAccount(transfer.getSourceAccountId(), AUTHORIZATION))
                .thenReturn(new AccountView(transfer.getSourceAccountId(), "EUR", "ACTIVE"));
        when(client.getBeneficiary(transfer.getBeneficiaryId(), AUTHORIZATION))
                .thenReturn(new BeneficiaryView(transfer.getBeneficiaryId(), DESTINATION_REFERENCE, "SUSPENDED"));

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.getFailureReason()).contains("Beneficiary is not active");
        verify(client, never()).reserve(any(), any(), any());
        verify(client, never()).debit(any(), any(), any());
    }

    @Test
    void serviceUnavailableDuringValidationFailsWithoutCompensation() {
        Transfer transfer = newTransfer();
        when(client.getAccount(transfer.getSourceAccountId(), AUTHORIZATION))
                .thenThrow(new ResourceAccessException("account-service is down"));

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.NOT_REQUIRED);
        assertThat(transfer.getFailureReason()).contains("ResourceAccessException", "account-service is down");
        verify(client, never()).reserve(any(), any(), any());
        verify(client, never()).debit(any(), any(), any());
    }

    @Test
    void debitFailureReleasesReservation() {
        Transfer transfer = newTransfer();
        stubValidSourceAndBeneficiary(transfer);
        doThrow(new TransferBusinessException("debit rejected"))
                .when(client).debit(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.COMPLETED);
        assertThat(transfer.getFailureReason()).contains("debit rejected", "reservation released");
        verify(client).release(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        verify(client, never()).creditDestination(anyString(), any(), anyString(), anyString());
    }

    @Test
    void creditFailureRefundsSource() {
        Transfer transfer = newTransfer();
        stubValidSourceAndBeneficiary(transfer);
        when(client.creditDestination(anyString(), any(), anyString(), anyString()))
                .thenThrow(new ResourceAccessException("destination unavailable"));

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.COMPLETED);
        assertThat(transfer.getFailureReason()).contains("destination unavailable", "source credited back");
        verify(client).creditSource(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        verify(client, never()).release(any(), any(), any());
        verify(transactionClient).recordReversal(transfer);
    }

    @Test
    void failedCompensationIsReported() {
        Transfer transfer = newTransfer();
        stubValidSourceAndBeneficiary(transfer);
        doThrow(new TransferBusinessException("debit rejected"))
                .when(client).debit(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        doThrow(new ResourceAccessException("release unavailable"))
                .when(client).release(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());

        orchestrator.execute(transfer, AUTHORIZATION);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.getCompensationStatus()).isEqualTo(CompensationStatus.FAILED);
        assertThat(transfer.getFailureReason()).contains("reservation release failed", "release unavailable");
    }

    private void stubValidSourceAndBeneficiary(Transfer transfer) {
        when(client.getAccount(transfer.getSourceAccountId(), AUTHORIZATION))
                .thenReturn(new AccountView(transfer.getSourceAccountId(), "EUR", "ACTIVE"));
        when(client.getBeneficiary(transfer.getBeneficiaryId(), AUTHORIZATION))
                .thenReturn(new BeneficiaryView(transfer.getBeneficiaryId(), DESTINATION_REFERENCE, "ACTIVE"));
    }

    private void assertHistory(TransferStatus... expectedStatuses) {
        ArgumentCaptor<TransferStatusHistory> captor = ArgumentCaptor.forClass(TransferStatusHistory.class);
        verify(historyRepository, org.mockito.Mockito.atLeast(expectedStatuses.length)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(TransferStatusHistory::getToStatus)
                .containsExactly(expectedStatuses);
    }

    private Transfer newTransfer() {
        return new Transfer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PENDING",
                new BigDecimal("25.00"),
                "EUR",
                "invoice-42",
                TransferStatus.REQUESTED,
                "key-" + UUID.randomUUID(),
                "fingerprint",
                Instant.now()
        );
    }
}
