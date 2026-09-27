package com.example.ledger.account;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    public AccountBalanceService(
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "accountBalances", key = "#accountId")
    public AccountBalanceResponse getAccountBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        AccountBalance balance = accountBalanceRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Balance not found"));

        return new AccountBalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getAccountType(),
                account.getStatus(),
                balance.getLedgerBalance(),
                balance.getAvailableBalance(),
                balance.getUpdatedAt()
        );
    }
}