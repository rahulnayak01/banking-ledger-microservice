package com.example.ledger.account;

import java.util.UUID;

public record BalanceChangedEvent(UUID accountId) {
}
