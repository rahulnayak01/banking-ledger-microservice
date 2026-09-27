package com.example.ledger.elasticsearch;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionSearchController {

    private final TransactionSearchService transactionSearchService;

    public TransactionSearchController(TransactionSearchService transactionSearchService) {
        this.transactionSearchService = transactionSearchService;
    }

    @GetMapping("/search")
    public List<TransactionDocument> search(
            @RequestParam(required = false) String account,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String type
    ) {
        if (account != null && !account.isBlank()) {
            return transactionSearchService.searchByAccount(account);
        }
        if (status != null && !status.isBlank()) {
            return transactionSearchService.searchByStatus(status);
        }
        if (currency != null && !currency.isBlank()) {
            return transactionSearchService.searchByCurrency(currency);
        }
        if (type != null && !type.isBlank()) {
            return transactionSearchService.searchByType(type);
        }
        return List.of();
    }

    @GetMapping("/search/date-range")
    public List<TransactionDocument> searchByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return transactionSearchService.searchByDateRange(from, to);
    }

    @GetMapping("/search/text")
    public List<TransactionDocument> searchText(@RequestParam String q) {
        return transactionSearchService.searchText(q);
    }
}