package com.ahmedkh.banking.transfer.repository;

import com.ahmedkh.banking.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByOwnerUserIdAndIdempotencyKey(UUID ownerUserId, String idempotencyKey);

    Optional<Transfer> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<Transfer> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);
}
