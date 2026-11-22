package com.ahmedkh.banking.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_beneficiaries_owner_reference",
                columnNames = {"owner_user_id", "destination_account_reference"}
        )
)
public class Beneficiary {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "destination_account_reference", nullable = false, length = 32)
    private String destinationAccountReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Beneficiary() {
    }

    public Beneficiary(
            UUID id,
            UUID ownerUserId,
            String displayName,
            String destinationAccountReference,
            BeneficiaryStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.displayName = displayName;
        this.destinationAccountReference = destinationAccountReference;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDestinationAccountReference() {
        return destinationAccountReference;
    }

    public void setDestinationAccountReference(String destinationAccountReference) {
        this.destinationAccountReference = destinationAccountReference;
    }

    public BeneficiaryStatus getStatus() {
        return status;
    }

    public void setStatus(BeneficiaryStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
