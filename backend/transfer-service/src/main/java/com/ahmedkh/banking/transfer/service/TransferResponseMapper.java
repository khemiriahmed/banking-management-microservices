package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.StatusChangeResponse;
import com.ahmedkh.banking.transfer.dto.TransferResponse;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatusHistory;
import com.ahmedkh.banking.transfer.repository.TransferStatusHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TransferResponseMapper {

    private final TransferStatusHistoryRepository historyRepository;

    public TransferResponseMapper(TransferStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public TransferResponse toResponse(Transfer transfer) {
        return TransferResponse.from(transfer, history(transfer.getId()));
    }

    public List<StatusChangeResponse> history(UUID transferId) {
        return historyRepository.findByTransferIdOrderByCreatedAtAsc(transferId).stream()
                .map(this::toStatusChange)
                .toList();
    }

    private StatusChangeResponse toStatusChange(TransferStatusHistory entry) {
        return new StatusChangeResponse(
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getReason(),
                entry.getCreatedAt()
        );
    }
}
