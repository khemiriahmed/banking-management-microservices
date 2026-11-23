package com.ahmedkh.banking.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_account_credits")
public class ExternalAccountCredit {

    @Id
    @Column(name = "operation_id", nullable = false, length = 128)
    private String operationId;

    @Column(name = "external_account_id", nullable = false)
    private UUID externalAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExternalAccountCredit() {
    }

    public ExternalAccountCredit(
            String operationId,
            UUID externalAccountId,
            BigDecimal amount,
            String currency,
            Instant createdAt
    ) {
        this.operationId = operationId;
        this.externalAccountId = externalAccountId;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public String getOperationId() {
        return operationId;
    }

    public UUID getExternalAccountId() {
        return externalAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
