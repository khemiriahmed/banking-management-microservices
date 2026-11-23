package com.ahmedkh.banking.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transaction_entries")
public class TransactionEntry {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_record_id", nullable = false)
    private TransactionRecord record;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "destination_reference", length = 64)
    private String destinationReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntrySide side;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected TransactionEntry() {
    }

    public TransactionEntry(
            UUID id,
            TransactionRecord record,
            UUID accountId,
            String destinationReference,
            EntrySide side,
            BigDecimal amount,
            String currency
    ) {
        this.id = id;
        this.record = record;
        this.accountId = accountId;
        this.destinationReference = destinationReference;
        this.side = side;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public TransactionRecord getRecord() {
        return record;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getDestinationReference() {
        return destinationReference;
    }

    public EntrySide getSide() {
        return side;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
