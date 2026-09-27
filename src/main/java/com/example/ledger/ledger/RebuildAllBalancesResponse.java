package com.example.ledger.ledger;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Result of rebuilding all account balances across the system.
 */
public record RebuildAllBalancesResponse(
        int totalAccountsProcessed,
        int totalEntriesReplayed,
        List<RebuildBalanceResponse> accounts,
        OffsetDateTime completedAt
) {
}
