package com.example.ledger.ledger;

public record CreateAccountRequest(
        String externalCustomerId,
        String accountNumber,
        String currency
) {
}