package com.example.ledger.ledger;

import com.example.ledger.account.*;
import com.example.ledger.transaction.TransactionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LedgerReplayService — reconstructs account balances directly from the immutable ledger.
 *
 * <p>Core Principle:
 * The ledger (ledger_entries) is the single authoritative source of truth.
 * Account balances (account_balances) are read projections that can be deleted
 * or corrupted and subsequently restored perfectly by replaying ledger history.
 */
@Service
public class LedgerReplayService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerReplayService(AccountRepository accountRepository,
                               AccountBalanceRepository accountBalanceRepository,
                               LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Rebuilds the balance for a specific account by account number.
     *
     * @param accountNumber the unique account number
     * @return summary of the rebuild operation
     */
    @Transactional
    public RebuildBalanceResponse rebuildBalanceByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
        return rebuildBalanceForAccount(account);
    }

    /**
     * Rebuilds the balance for a specific account by account ID.
     *
     * @param accountId the account UUID
     * @return summary of the rebuild operation
     */
    @Transactional
    public RebuildBalanceResponse rebuildBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        return rebuildBalanceForAccount(account);
    }

    /**
     * Rebuilds balances for all registered accounts across the entire ledger.
     *
     * @return summary of the batch rebuild operation
     */
    @Transactional
    public RebuildAllBalancesResponse rebuildAllBalances() {
        List<Account> allAccounts = accountRepository.findAll();
        List<RebuildBalanceResponse> results = new ArrayList<>();
        int totalEntriesReplayed = 0;

        for (Account account : allAccounts) {
            RebuildBalanceResponse result = rebuildBalanceForAccount(account);
            results.add(result);
            totalEntriesReplayed += result.totalEntriesReplayed();
        }

        return new RebuildAllBalancesResponse(
                allAccounts.size(),
                totalEntriesReplayed,
                results,
                OffsetDateTime.now()
        );
    }

    /**
     * Internal replay logic for a given account.
     */
    private RebuildBalanceResponse rebuildBalanceForAccount(Account account) {
        // 1. Acquire pessimistic write lock on the account's balance record
        AccountBalance balance = accountBalanceRepository.findByIdForUpdate(account.getId())
                .orElseGet(() -> new AccountBalance(account.getId()));

        BigDecimal previousBalance = balance.getAvailableBalance();

        // 2. Fetch all immutable ledger entries for this account in chronological order
        List<LedgerEntry> entries = entryRepository.findByAccountIdOrderByCreatedAtAsc(account.getId());

        // 3. Reset projection state to 0
        balance.reset();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        int replayedCount = 0;

        // 4. Replay entries from COMPLETED transactions
        for (LedgerEntry entry : entries) {
            if (entry.getTransaction().getStatus() == TransactionStatus.COMPLETED) {
                balance.apply(account.getAccountType(), entry.getEntryType(), entry.getAmount());
                if (entry.getEntryType() == EntryType.DEBIT) {
                    totalDebits = totalDebits.add(entry.getAmount());
                } else if (entry.getEntryType() == EntryType.CREDIT) {
                    totalCredits = totalCredits.add(entry.getAmount());
                }
                replayedCount++;
            }
        }

        // 5. Persist the restored projection
        accountBalanceRepository.save(balance);

        BigDecimal rebuiltBalance = balance.getAvailableBalance();
        String status = previousBalance.compareTo(rebuiltBalance) == 0 ? "MATCH" : "RESTORED";

        return new RebuildBalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                previousBalance,
                rebuiltBalance,
                replayedCount,
                totalDebits,
                totalCredits,
                status,
                OffsetDateTime.now()
        );
    }
}
