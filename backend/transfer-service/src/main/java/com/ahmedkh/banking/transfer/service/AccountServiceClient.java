package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.AccountView;
import com.ahmedkh.banking.transfer.dto.BeneficiaryView;
import com.ahmedkh.banking.transfer.dto.InternalAccountResponse;
import com.ahmedkh.banking.transfer.dto.InternalExternalCreditResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountServiceClient {

    AccountView getAccount(UUID accountId, String authorization);

    BeneficiaryView getBeneficiary(UUID beneficiaryId, String authorization);

    InternalAccountResponse reserve(UUID accountId, BigDecimal amount, String currency);

    void debit(UUID accountId, BigDecimal amount, String currency);

    void creditSource(UUID accountId, BigDecimal amount, String currency);

    void release(UUID accountId, BigDecimal amount, String currency);

    InternalExternalCreditResponse creditDestination(
            String destinationReference,
            BigDecimal amount,
            String currency,
            String operationId
    );
}
