package com.example.ledger.account;

import com.example.ledger.ledger.CreateAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;

    public AccountController(AccountService accountService, AccountBalanceService accountBalanceService) {
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @RequestBody CreateAccountRequest request) {

        Account account = accountService.createAccount(request);

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getStatus(),
                account.getAccountType()
        );
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        Account account = accountService.getAccount(id);
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getStatus(),
                account.getAccountType()
        );
    }

    @GetMapping("/{id}/balance")
    public AccountBalanceResponse getBalance(@PathVariable UUID id) {
        return accountBalanceService.getAccountBalance(id);
    }

    @GetMapping("/by-number/{accountNumber}/balance")
    public AccountBalanceResponse getBalanceByAccountNumber(@PathVariable String accountNumber) {
        return accountService.getAccountBalanceByAccountNumber(accountNumber);
    }

    public record AccountResponse(
            UUID id,
            String accountNumber,
            String currency,
            AccountStatus status,
            AccountType accountType
    ) {
    }
}