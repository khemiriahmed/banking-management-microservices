package com.ahmedkh.banking.transfer.dto;

import com.ahmedkh.banking.transfer.entity.TransferStatus;

import java.time.Instant;

public record StatusChangeResponse(
        TransferStatus from,
        TransferStatus to,
        String reason,
        Instant createdAt
) {
}
