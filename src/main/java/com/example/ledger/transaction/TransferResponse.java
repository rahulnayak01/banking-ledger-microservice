package com.example.ledger.transaction;

import com.example.ledger.ledger.EntryType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response returned after a double-entry transfer is posted.
 */
public record TransferResponse(
        UUID transactionId,
        TransactionType transactionType,
        TransactionStatus status,
        List<LedgerEntryView> entries,
        OffsetDateTime createdAt
) {

    /**
     * A read-only projection of a single ledger entry for the API response.
     */
    public record LedgerEntryView(
            UUID entryId,
            String accountNumber,
            EntryType entryType,
            BigDecimal amount,
            String currency
    ) {
    }
}
