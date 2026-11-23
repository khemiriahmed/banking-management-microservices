package com.ahmedkh.banking.transaction.repository;

import com.ahmedkh.banking.transaction.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {

    Optional<TransactionRecord> findByExternalReference(String externalReference);

    Optional<TransactionRecord> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    Page<TransactionRecord> findByOwnerUserIdAndOccurredAtBetween(
            UUID ownerUserId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    Page<TransactionRecord> findByOwnerUserIdAndEntries_AccountIdAndOccurredAtBetween(
            UUID ownerUserId,
            UUID accountId,
            Instant from,
            Instant to,
            Pageable pageable
    );
}
