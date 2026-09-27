package com.example.ledger.transaction;

import java.math.BigDecimal;

/**
 * Request payload for POST /ledger/deposit.
 */
public record DepositRequest(
        String accountNumber,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
