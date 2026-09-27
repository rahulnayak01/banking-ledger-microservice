package com.example.ledger.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TransactionSearchService {

    private final TransactionSearchRepository transactionSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public TransactionSearchService(
            TransactionSearchRepository transactionSearchRepository,
            ElasticsearchOperations elasticsearchOperations
    ) {
        this.transactionSearchRepository = transactionSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<TransactionDocument> searchByAccount(String accountNumber) {
        return transactionSearchRepository.findByDebitAccountNumberOrCreditAccountNumber(
                accountNumber,
                accountNumber
        );
    }

    public List<TransactionDocument> searchByStatus(String status) {
        return transactionSearchRepository.findByStatus(status);
    }

    public List<TransactionDocument> searchByCurrency(String currency) {
        return transactionSearchRepository.findByCurrency(currency);
    }

    public List<TransactionDocument> searchByType(String type) {
        return transactionSearchRepository.findByTransactionType(type);
    }

    public List<TransactionDocument> searchByDateRange(OffsetDateTime from, OffsetDateTime to) {
        return transactionSearchRepository.findByCreatedAtBetween(from, to);
    }

    public List<TransactionDocument> searchText(String q) {

        Query query = new BoolQuery.Builder()
                .should(s -> s.multiMatch(mm -> mm
                        .query(q)
                        .fields(
                                "transactionType",
                                "status",
                                "currency",
                                "debitAccountNumber",
                                "creditAccountNumber"
                        )
                        .fuzziness("AUTO")
                        .type(TextQueryType.BestFields)
                ))
                .minimumShouldMatch("1")
                .build()
                ._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();

        SearchHits<TransactionDocument> hits =
                elasticsearchOperations.search(
                        nativeQuery,
                        TransactionDocument.class
                );

        return hits.stream()
                .map(hit -> hit.getContent())
                .toList();
    }
}