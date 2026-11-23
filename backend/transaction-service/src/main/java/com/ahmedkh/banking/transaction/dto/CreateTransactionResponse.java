package com.ahmedkh.banking.transaction.dto;

public record CreateTransactionResponse(
        TransactionResponse transaction,
        boolean alreadyProcessed
) {
}
