package com.ahmedkh.banking.account.repository;

import com.ahmedkh.banking.account.entity.ExternalAccountCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExternalAccountCreditRepository extends JpaRepository<ExternalAccountCredit, String> {

    boolean existsByOperationId(String operationId);
}
