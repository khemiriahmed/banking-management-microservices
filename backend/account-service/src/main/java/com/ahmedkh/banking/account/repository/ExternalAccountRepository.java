package com.ahmedkh.banking.account.repository;

import com.ahmedkh.banking.account.entity.ExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, UUID> {

    Optional<ExternalAccount> findByDestinationReference(String destinationReference);
}
