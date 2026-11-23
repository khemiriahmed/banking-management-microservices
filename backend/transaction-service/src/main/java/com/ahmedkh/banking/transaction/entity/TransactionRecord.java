package com.ahmedkh.banking.transaction.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transaction_records")
public class TransactionRecord {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "external_reference", nullable = false, unique = true)
    private String externalReference;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionEntry> entries = new ArrayList<>();

    protected TransactionRecord() {
    }

    public TransactionRecord(
            UUID id,
            String externalReference,
            UUID transferId,
            UUID ownerUserId,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            String currency,
            Instant occurredAt,
            Instant createdAt
    ) {
        this.id = id;
        this.externalReference = externalReference;
        this.transferId = transferId;
        this.ownerUserId = ownerUserId;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TransactionEntry> getEntries() {
        return entries;
    }

    public void addEntry(TransactionEntry entry) {
        entries.add(entry);
    }
}
