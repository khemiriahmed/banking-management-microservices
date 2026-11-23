package com.ahmedkh.banking.transfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "beneficiary_id", nullable = false)
    private UUID beneficiaryId;

    @Column(name = "destination_reference", nullable = false, length = 64)
    private String destinationReference;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_status", nullable = false, length = 20)
    private CompensationStatus compensationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Transfer() {
    }

    public Transfer(
            UUID id,
            UUID ownerUserId,
            UUID sourceAccountId,
            UUID beneficiaryId,
            String destinationReference,
            BigDecimal amount,
            String currency,
            String reason,
            TransferStatus status,
            String idempotencyKey,
            String requestFingerprint,
            Instant createdAt
    ) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.sourceAccountId = sourceAccountId;
        this.beneficiaryId = beneficiaryId;
        this.destinationReference = destinationReference;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.compensationStatus = CompensationStatus.NONE;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getBeneficiaryId() {
        return beneficiaryId;
    }

    public String getDestinationReference() {
        return destinationReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReason() {
        return reason;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public CompensationStatus getCompensationStatus() {
        return compensationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setDestinationReference(String destinationReference) {
        this.destinationReference = destinationReference;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setCompensationStatus(CompensationStatus compensationStatus) {
        this.compensationStatus = compensationStatus;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
