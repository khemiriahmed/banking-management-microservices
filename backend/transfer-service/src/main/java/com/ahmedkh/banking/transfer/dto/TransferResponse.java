package com.ahmedkh.banking.transfer.dto;

import com.ahmedkh.banking.transfer.entity.CompensationStatus;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID ownerUserId,
        UUID sourceAccountId,
        UUID beneficiaryId,
        String destinationReference,
        BigDecimal amount,
        String currency,
        String reason,
        TransferStatus status,
        String failureReason,
        CompensationStatus compensationStatus,
        Instant createdAt,
        Instant updatedAt,
        List<StatusChangeResponse> history
) {

    public static TransferResponse from(Transfer transfer, List<StatusChangeResponse> history) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getOwnerUserId(),
                transfer.getSourceAccountId(),
                transfer.getBeneficiaryId(),
                transfer.getDestinationReference(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getReason(),
                transfer.getStatus(),
                transfer.getFailureReason(),
                transfer.getCompensationStatus(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                history
        );
    }
}
