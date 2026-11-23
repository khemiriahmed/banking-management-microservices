package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.AccountView;
import com.ahmedkh.banking.transfer.dto.BeneficiaryView;
import com.ahmedkh.banking.transfer.entity.CompensationStatus;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import com.ahmedkh.banking.transfer.entity.TransferStatusHistory;
import com.ahmedkh.banking.transfer.exception.InvalidTransferStateException;
import com.ahmedkh.banking.transfer.exception.TransferBusinessException;
import com.ahmedkh.banking.transfer.repository.TransferRepository;
import com.ahmedkh.banking.transfer.repository.TransferStatusHistoryRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class TransferOrchestrator {

    private final AccountServiceClient client;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository historyRepository;

    public TransferOrchestrator(
            AccountServiceClient client,
            TransferRepository transferRepository,
            TransferStatusHistoryRepository historyRepository
    ) {
        this.client = client;
        this.transferRepository = transferRepository;
        this.historyRepository = historyRepository;
    }

    public void execute(Transfer transfer, String authorization) {
        transition(transfer, TransferStatus.VALIDATING, "Validating source account and beneficiary");

        try {
            AccountView account = client.getAccount(transfer.getSourceAccountId(), authorization);
            if (!"ACTIVE".equals(account.status())) {
                throw new TransferBusinessException("Source account is not active");
            }
            if (!transfer.getCurrency().equals(account.currency())) {
                throw new TransferBusinessException("Transfer currency does not match source account");
            }

            BeneficiaryView beneficiary = client.getBeneficiary(transfer.getBeneficiaryId(), authorization);
            if (!"ACTIVE".equals(beneficiary.status())) {
                throw new TransferBusinessException("Beneficiary is not active");
            }
            if (beneficiary.destinationAccountReference() == null || beneficiary.destinationAccountReference().isBlank()) {
                throw new TransferBusinessException("Beneficiary destination reference is missing");
            }
            transfer.setDestinationReference(beneficiary.destinationAccountReference());

            transition(transfer, TransferStatus.PROCESSING, "Source account and beneficiary validated");
            client.reserve(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        } catch (RuntimeException exception) {
            transfer.setCompensationStatus(CompensationStatus.NOT_REQUIRED);
            fail(transfer, "Validation or reservation failed: " + safeMessage(exception));
            return;
        }

        try {
            client.debit(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
        } catch (RuntimeException exception) {
            compensateRelease(transfer, exception);
            return;
        }

        String operationId = transfer.getId() + ":credit";
        try {
            client.creditDestination(
                    transfer.getDestinationReference(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    operationId
            );
        } catch (RuntimeException exception) {
            compensateSourceCredit(transfer, exception);
            return;
        }

        transfer.setCompensationStatus(CompensationStatus.NOT_REQUIRED);
        transition(transfer, TransferStatus.COMPLETED, "Source debited and destination credited");
    }

    private void compensateRelease(Transfer transfer, RuntimeException cause) {
        transfer.setCompensationStatus(CompensationStatus.PENDING);
        transferRepository.save(transfer);
        String compensation;
        try {
            client.release(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
            transfer.setCompensationStatus(CompensationStatus.COMPLETED);
            compensation = "reservation released";
        } catch (RuntimeException compensationFailure) {
            transfer.setCompensationStatus(CompensationStatus.FAILED);
            compensation = "reservation release failed: " + safeMessage(compensationFailure);
        }
        fail(transfer, "Debit failed: " + safeMessage(cause) + "; compensation " + compensation);
    }

    private void compensateSourceCredit(Transfer transfer, RuntimeException cause) {
        transfer.setCompensationStatus(CompensationStatus.PENDING);
        transferRepository.save(transfer);
        String compensation;
        try {
            client.creditSource(transfer.getSourceAccountId(), transfer.getAmount(), transfer.getCurrency());
            transfer.setCompensationStatus(CompensationStatus.COMPLETED);
            compensation = "source credited back";
        } catch (RuntimeException compensationFailure) {
            transfer.setCompensationStatus(CompensationStatus.FAILED);
            compensation = "source refund failed: " + safeMessage(compensationFailure);
        }
        fail(transfer, "Destination credit failed: " + safeMessage(cause) + "; compensation " + compensation);
    }

    private void fail(Transfer transfer, String reason) {
        transfer.setFailureReason(truncate(reason));
        transition(transfer, TransferStatus.FAILED, truncate(reason));
    }

    private void transition(Transfer transfer, TransferStatus target, String reason) {
        TransferStatus current = transfer.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidTransferStateException(
                    "Transfer status " + current + " cannot transition to " + target
            );
        }
        transfer.setStatus(target);
        transfer.touch();
        transferRepository.save(transfer);
        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(),
                transfer.getId(),
                current,
                target,
                reason == null ? null : truncate(reason),
                Instant.now()
        ));
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
