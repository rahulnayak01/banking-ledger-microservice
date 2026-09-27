package com.example.ledger.transaction;

import java.math.BigDecimal;

/**
 * Request payload for POST /ledger/withdraw.
 */
public record WithdrawRequest(
        String accountNumber,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
