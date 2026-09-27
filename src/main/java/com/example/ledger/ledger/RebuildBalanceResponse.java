package com.example.ledger.ledger;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Result of a single account balance rebuild from the ledger.
 */
public record RebuildBalanceResponse(
        UUID accountId,
        String accountNumber,
        BigDecimal previousBalance,
        BigDecimal rebuiltBalance,
        int totalEntriesReplayed,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        String status,
        OffsetDateTime replayedAt
) {
}
