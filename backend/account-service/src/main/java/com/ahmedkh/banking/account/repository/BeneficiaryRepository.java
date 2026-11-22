package com.ahmedkh.banking.account.repository;

import com.ahmedkh.banking.account.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    Optional<Beneficiary> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    boolean existsByOwnerUserIdAndDestinationAccountReference(UUID ownerUserId, String destinationAccountReference);

    boolean existsByOwnerUserIdAndDestinationAccountReferenceAndIdNot(
            UUID ownerUserId,
            String destinationAccountReference,
            UUID id
    );
}
