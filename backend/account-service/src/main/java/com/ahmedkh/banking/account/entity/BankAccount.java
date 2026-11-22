package com.ahmedkh.banking.account.entity;

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
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "account_number_masked", nullable = false, updatable = false, unique = true, length = 32)
    private String accountNumberMasked;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, updatable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedBalance;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BankAccount() {
    }

    public BankAccount(
            UUID id,
            UUID ownerUserId,
            String accountNumberMasked,
            AccountType accountType,
            String currency,
            AccountStatus status,
            BigDecimal balance,
            BigDecimal reservedBalance,
            Instant createdAt
    ) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.accountNumberMasked = accountNumberMasked;
        this.accountType = accountType;
        this.currency = currency;
        this.status = status;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public void setReservedBalance(BigDecimal reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
