package com.example.ledger.account;

import com.example.ledger.ledger.EntryType;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity
@Table(name = "account_balances")
public class AccountBalance implements Persistable<UUID> {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(
            name = "ledger_balance",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(
            name = "available_balance",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    protected AccountBalance() {
    }

    public AccountBalance(UUID accountId) {
        this.accountId = accountId;
        this.ledgerBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.ledgerBalance = this.ledgerBalance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        this.ledgerBalance = this.ledgerBalance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    public void reset() {
        this.ledgerBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return false;
        }
        return this.availableBalance.compareTo(amount) >= 0;
    }

    public void apply(AccountType accountType, EntryType entryType, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (accountType == AccountType.CUSTOMER) {
            if (entryType == EntryType.CREDIT) {
                credit(amount);
            } else {
                debit(amount);
            }
        } else {
            // For system accounts (e.g. BANK-CASH):
            // DEBIT increases bank cash asset (money deposited into bank)
            // CREDIT decreases bank cash asset (money paid out of bank)
            if (entryType == EntryType.DEBIT) {
                credit(amount);
            } else {
                debit(amount);
            }
        }
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public UUID getId() {
        return accountId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    private void markNotNew() {
        isNew = false;
    }
}