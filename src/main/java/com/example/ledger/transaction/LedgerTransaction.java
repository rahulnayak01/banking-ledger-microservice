package com.example.ledger.transaction;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(
            TransactionType transactionType,
            String idempotencyKey,
            String requestHash
    ) {
        this.transactionType = transactionType;
        this.status = TransactionStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = OffsetDateTime.now();
    }

    public LedgerTransaction(
            TransactionType transactionType,
            String idempotencyKey
    ) {
        this(transactionType, idempotencyKey, null);
    }

    public UUID getId() {
        return id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    // --- domain state transitions ---

    /**
     * Marks the transaction as successfully completed.
     * Should be called after all ledger entries have been persisted
     * and the DB transaction is about to commit.
     */
    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
    }

    /**
     * Marks the transaction as failed.
     * Should be called inside the catch block before re-throwing,
     * while still inside the same DB transaction.
     */
    public void markFailed() {
        this.status = TransactionStatus.FAILED;
    }
}