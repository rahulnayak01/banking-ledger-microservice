package com.example.ledger.ledger;

import com.example.ledger.transaction.LedgerTransaction;
import com.example.ledger.account.Account;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(
            LedgerTransaction transaction,
            Account account,
            EntryType entryType,
            BigDecimal amount,
            String currency
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Ledger entry amount must be positive"
            );
        }

        this.transaction = transaction;
        this.account = account;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public Account getAccount() {
        return account;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}