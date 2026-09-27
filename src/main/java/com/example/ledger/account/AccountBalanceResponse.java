package com.example.ledger.account;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountBalanceResponse(
        UUID accountId,
        String accountNumber,
        String currency,
        AccountType accountType,
        AccountStatus status,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance,
        OffsetDateTime updatedAt
) {
}
