package com.ahmedkh.banking.transfer.repository;

import com.ahmedkh.banking.transfer.entity.TransferStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferStatusHistoryRepository extends JpaRepository<TransferStatusHistory, UUID> {

    List<TransferStatusHistory> findByTransferIdOrderByCreatedAtAsc(UUID transferId);
}
