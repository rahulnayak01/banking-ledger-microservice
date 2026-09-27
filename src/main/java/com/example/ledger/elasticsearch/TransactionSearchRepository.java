package com.example.ledger.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionSearchRepository
        extends ElasticsearchRepository<TransactionDocument, String> {

    List<TransactionDocument> findByDebitAccountNumberOrCreditAccountNumber(
            String debitAccountNumber,
            String creditAccountNumber
    );

    List<TransactionDocument> findByDebitAccountIdOrCreditAccountId(
            String debitAccountId,
            String creditAccountId
    );

    List<TransactionDocument> findByCreatedAtBetween(
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<TransactionDocument> findByStatus(String status);
    List<TransactionDocument> findByCurrency(String currency);
    List<TransactionDocument> findByTransactionType(String transactionType);
}