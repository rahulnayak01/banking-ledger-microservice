package com.example.ledger.transaction;

import java.math.BigDecimal;

/**
 * Request payload for POST /ledger/transfer.
 *
 * <p>debitAccountId  — account that will be DEBITed (money leaves)
 * <p>creditAccountId — account that will be CREDITed (money arrives)
 * <p>amount          — must be > 0
 * <p>currency        — 3-letter ISO code, e.g. "INR"
 * <p>idempotencyKey  — caller-supplied unique key; deduplication handled in Phase 6
 */
public record TransferRequest(
        String debitAccountNumber,
        String creditAccountNumber,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
