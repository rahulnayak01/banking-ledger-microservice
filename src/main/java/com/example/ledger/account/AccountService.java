package com.example.ledger.account;

import com.example.ledger.customer.Customer;
import com.example.ledger.customer.CustomerRepository;
import com.example.ledger.ledger.CreateAccountRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountBalanceService accountBalanceService;

    public AccountService(CustomerRepository customerRepository,
                          AccountRepository accountRepository,
                          AccountBalanceRepository accountBalanceRepository, AccountBalanceService accountBalanceService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountBalanceService = accountBalanceService;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {

        Customer customer = customerRepository
                .findByExternalCustomerId(request.externalCustomerId())
                .orElseGet(() ->
                        customerRepository.save(
                                new Customer(request.externalCustomerId())
                        )
                );

        Account account = new Account(
                customer,
                request.accountNumber(),
                request.currency(),
                AccountType.CUSTOMER
        );

        account = accountRepository.save(account);
        AccountBalance balance = new AccountBalance(account.getId());
        accountBalanceRepository.save(balance);
        return account;
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

//    @Transactional(readOnly = true)
//    @Cacheable(value = "accountBalances",key = "#accountId")
//    public AccountBalanceResponse getAccountBalance(UUID accountId) {
//        Account account = getAccount(accountId);
//        AccountBalance balance = accountBalanceRepository.findById(accountId)
//                .orElseThrow(() -> new IllegalArgumentException("Balance record not found for account: " + accountId));
//
//        return new AccountBalanceResponse(
//                account.getId(),
//                account.getAccountNumber(),
//                account.getCurrency(),
//                account.getAccountType(),
//                account.getStatus(),
//                balance.getLedgerBalance(),
//                balance.getAvailableBalance(),
//                balance.getUpdatedAt()
//        );
//    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalanceByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        return accountBalanceService.getAccountBalance(account.getId());
    }
}