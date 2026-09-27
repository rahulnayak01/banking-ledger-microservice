package com.example.ledger.transaction;

import com.example.ledger.account.*;
import com.example.ledger.ledger.EntryType;
import com.example.ledger.ledger.LedgerEntry;
import com.example.ledger.ledger.LedgerEntryRepository;
import com.example.ledger.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * LedgerService — the double-entry accounting engine.
 *
 * <p>Core invariants enforced here:
 * <ul>
 *   <li>Amount must be > 0</li>
 *   <li>Debit account and credit account must both be ACTIVE</li>
 *   <li>Debit account and credit account must share the same currency</li>
 *   <li>Debit account must have sufficient funds (for customer accounts)</li>
 *   <li>Every posted transaction must have exactly balanced entries
 *       (total DEBIT == total CREDIT)</li>
 *   <li>Account balances are atomically updated in the same transaction</li>
 *   <li>Transactional Outbox event is saved within the same DB transaction</li>
 *   <li>The entire operation is atomic — if anything fails, everything
 *       rolls back via {@code @Transactional}</li>
 *   <li>Ledger entries are written as immutable records (no setters on
 *       {@link LedgerEntry})</li>
 * </ul>
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final OutboxService outboxService;
    private final BalanceCacheInvalidationService balanceCacheInvalidationService;

    public LedgerService(AccountRepository accountRepository,
                         AccountBalanceRepository accountBalanceRepository,
                         LedgerTransactionRepository transactionRepository,
                         LedgerEntryRepository entryRepository,
                         OutboxService outboxService, BalanceCacheInvalidationService balanceCacheInvalidationService) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.outboxService = outboxService;
        this.balanceCacheInvalidationService = balanceCacheInvalidationService;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Posts a double-entry transfer between two accounts.
     *
     * <p>What happens inside one DB transaction:
     * <ol>
     *   <li>Validate the request (amount, accounts, currency match)</li>
     *   <li>Load and validate account balances (insufficient funds check)</li>
     *   <li>Create a {@link LedgerTransaction} with status PENDING</li>
     *   <li>Create a DEBIT {@link LedgerEntry} for the source account</li>
     *   <li>Create a CREDIT {@link LedgerEntry} for the destination account</li>
     *   <li>Assert that entries balance (total DEBIT == total CREDIT)</li>
     *   <li>Apply and save balance updates atomically</li>
     *   <li>Mark transaction as COMPLETED</li>
     * </ol>
     *
     * <p>If any step throws, Spring rolls back the entire DB transaction —
     * no partial state is committed.
     *
     * @param request the transfer details
     * @return a {@link TransferResponse} with the transaction and its entries
     */
    @Transactional
    public TransferResponse postTransfer(TransferRequest request) {

        // --- 0. Idempotency check ---
        String idempotencyKey = request.idempotencyKey();
        String requestHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = computePayloadHash(request);
            var existingOpt = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                LedgerTransaction existing = existingOpt.get();

                // Detect payload conflict (same key used with different request parameters)
                if (existing.getRequestHash() != null && !existing.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(
                            "Idempotency key '" + idempotencyKey + "' was already used with different request parameters."
                    );
                }

                // If completed, return the original transaction result
                if (existing.getStatus() == TransactionStatus.COMPLETED) {
                    return buildExistingResponse(existing);
                }

                if (existing.getStatus() == TransactionStatus.PENDING) {
                    throw new IllegalStateException(
                            "A transaction with idempotency key '" + idempotencyKey + "' is currently being processed. Please retry shortly."
                    );
                }

                throw new IllegalStateException(
                        "A previous transaction with idempotency key '" + idempotencyKey + "' failed."
                );
            }
        }

        // --- 1. Validate amount ---
        validateAmount(request.amount());

        // --- 2. Load accounts ---
        Account debitAccount = findActiveAccount(request.debitAccountNumber());
        Account creditAccount = findActiveAccount(request.creditAccountNumber());

        // --- 3. Prevent self-transfer ---
        if (debitAccount.getId().equals(creditAccount.getId())) {
            throw new IllegalArgumentException(
                    "Debit and credit accounts must be different"
            );
        }

        // --- 4. Currency must match on both accounts ---
        validateCurrencyMatch(debitAccount, creditAccount, request.currency());

        // --- 5. Load and lock account balances (deterministic lock acquisition order) ---
        //
        // Concurrency & Deadlock Prevention:
        // When two concurrent transfers happen in opposite directions (e.g. A -> B and B -> A),
        // locking in request order creates a circular wait (PostgreSQL deadlock).
        // Sorting the IDs guarantees a single global locking order, eliminating deadlocks.
        UUID firstLockId = debitAccount.getId().compareTo(creditAccount.getId()) < 0
                ? debitAccount.getId()
                : creditAccount.getId();
        UUID secondLockId = debitAccount.getId().compareTo(creditAccount.getId()) < 0
                ? creditAccount.getId()
                : debitAccount.getId();

        AccountBalance firstBalance = accountBalanceRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new IllegalStateException(
                        "Balance record not found for account: " + firstLockId
                ));

        AccountBalance secondBalance = accountBalanceRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new IllegalStateException(
                        "Balance record not found for account: " + secondLockId
                ));

        AccountBalance debitBalance = debitAccount.getId().equals(firstLockId) ? firstBalance : secondBalance;
        AccountBalance creditBalance = creditAccount.getId().equals(firstLockId) ? firstBalance : secondBalance;

        // Enforce sufficient funds on customer debit account (prevent overdraft)
        if (debitAccount.getAccountType() == AccountType.CUSTOMER) {
            if (!debitBalance.hasSufficientFunds(request.amount())) {
                throw new InsufficientFundsException(
                        "Insufficient funds in account " + debitAccount.getAccountNumber()
                                + ": available=" + debitBalance.getAvailableBalance()
                                + ", requested=" + request.amount()
                );
            }
        }

        // --- 6. Create LedgerTransaction (PENDING) ---
        LedgerTransaction transaction = new LedgerTransaction(
                TransactionType.TRANSFER,
                idempotencyKey,
                requestHash
        );
        transaction = transactionRepository.save(transaction);

        // --- 7. Create ledger entries ---
        LedgerEntry debitEntry = new LedgerEntry(
                transaction,
                debitAccount,
                EntryType.DEBIT,
                request.amount(),
                request.currency()
        );

        LedgerEntry creditEntry = new LedgerEntry(
                transaction,
                creditAccount,
                EntryType.CREDIT,
                request.amount(),
                request.currency()
        );

        debitEntry = entryRepository.save(debitEntry);
        creditEntry = entryRepository.save(creditEntry);

        // --- 8. Assert double-entry balance invariant ---
        assertBalanced(List.of(debitEntry, creditEntry));

        // --- 9. Apply balance updates atomically ---
        debitBalance.apply(debitAccount.getAccountType(), EntryType.DEBIT, request.amount());
        creditBalance.apply(creditAccount.getAccountType(), EntryType.CREDIT, request.amount());

        accountBalanceRepository.save(debitBalance);
        accountBalanceRepository.save(creditBalance);

        //inavalidating the caffeine cache
        balanceCacheInvalidationService
                .evictAfterCommit(debitAccount.getId());

        balanceCacheInvalidationService
                .evictAfterCommit(creditAccount.getId());

        // --- 10. Mark completed ---
        transaction.markCompleted();
        final LedgerTransaction saved = transactionRepository.save(transaction);

        // --- 11. Transactional Outbox (committed atomically with the financial ledger) ---
        String eventPayload = String.format(
                "{\"transactionId\":\"%s\",\"transactionType\":\"%s\",\"status\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"debitAccountNumber\":\"%s\",\"creditAccountNumber\":\"%s\",\"createdAt\":\"%s\"}",
                saved.getId(),
                saved.getTransactionType(),
                saved.getStatus(),
                request.amount().toPlainString(),
                request.currency(),
                debitAccount.getAccountNumber(),
                creditAccount.getAccountNumber(),
                saved.getCreatedAt()
        );

        outboxService.saveEvent(
                "TRANSACTION",
                saved.getId(),
                "TransactionCompleted",
                eventPayload
        );

        // --- 12. Build response ---
        return toResponse(saved, debitAccount, creditAccount, debitEntry, creditEntry);
    }

    /**
     * Posts a deposit transaction.
     *
     * <p>A deposit means money enters the system.
     * Double-entry:
     * <pre>
     *   DEBIT  BANK-CASH   (bank's cash increases)
     *   CREDIT CUSTOMER    (customer's balance increases)
     * </pre>
     */
    @Transactional
    public TransferResponse postDeposit(DepositRequest request) {
        // Under the hood, a deposit is just a transfer from the system account
        // to the customer's account.
        TransferRequest internalTransfer = new TransferRequest(
                SystemAccountInitializer.BANK_CASH_ACCOUNT_NUMBER,
                request.accountNumber(),
                request.amount(),
                request.currency(),
                request.idempotencyKey()
        );
        return postTransfer(internalTransfer);
    }

    /**
     * Posts a withdrawal transaction.
     *
     * <p>A withdrawal means money leaves the system.
     * Double-entry:
     * <pre>
     *   DEBIT  CUSTOMER    (customer's balance decreases)
     *   CREDIT BANK-CASH   (bank's cash decreases)
     * </pre>
     */
    @Transactional
    public TransferResponse postWithdraw(WithdrawRequest request) {
        // Under the hood, a withdrawal is just a transfer from the customer's
        // account to the system account.
        TransferRequest internalTransfer = new TransferRequest(
                request.accountNumber(),
                SystemAccountInitializer.BANK_CASH_ACCOUNT_NUMBER,
                request.amount(),
                request.currency(),
                request.idempotencyKey()
        );
        return postTransfer(internalTransfer);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }
    }

    private Account findActiveAccount(String accountNumber) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + accountNumber
                ));

        if (account.getStatus().name().equals("BLOCKED") ||
                account.getStatus().name().equals("CLOSED")) {
            throw new IllegalStateException(
                    "Account is not active: " + accountNumber
            );
        }

        return account;
    }

    private void validateCurrencyMatch(Account debit,
                                       Account credit,
                                       String requestCurrency) {
        if (!debit.getCurrency().equals(credit.getCurrency())) {
            throw new IllegalArgumentException(
                    "Currency mismatch between accounts: "
                            + debit.getCurrency() + " vs " + credit.getCurrency()
            );
        }
        if (!debit.getCurrency().equals(requestCurrency)) {
            throw new IllegalArgumentException(
                    "Request currency [" + requestCurrency
                            + "] does not match account currency ["
                            + debit.getCurrency() + "]"
            );
        }
    }

    /**
     * Core double-entry invariant check.
     *
     * <p>For any posted transaction:
     * <pre>
     *   SUM(DEBIT entry amounts) == SUM(CREDIT entry amounts)
     * </pre>
     *
     * <p>If this check fails it means we have a programming error —
     * the ledger would be out of balance. We throw immediately so the
     * {@code @Transactional} annotation causes a full rollback.
     */
    private void assertBalanced(List<LedgerEntry> entries) {
        BigDecimal totalDebits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            // This will cause @Transactional to roll back everything
            throw new IllegalStateException(
                    "Ledger is unbalanced: debits=" + totalDebits
                            + " credits=" + totalCredits
            );
        }
    }

    private TransferResponse toResponse(LedgerTransaction txn,
                                        Account debitAccount,
                                        Account creditAccount,
                                        LedgerEntry debitEntry,
                                        LedgerEntry creditEntry) {

        TransferResponse.LedgerEntryView debitView = new TransferResponse.LedgerEntryView(
                debitEntry.getId(),
                debitAccount.getAccountNumber(),
                debitEntry.getEntryType(),
                debitEntry.getAmount(),
                debitEntry.getCurrency()
        );

        TransferResponse.LedgerEntryView creditView = new TransferResponse.LedgerEntryView(
                creditEntry.getId(),
                creditAccount.getAccountNumber(),
                creditEntry.getEntryType(),
                creditEntry.getAmount(),
                creditEntry.getCurrency()
        );

        return new TransferResponse(
                txn.getId(),
                txn.getTransactionType(),
                txn.getStatus(),
                List.of(debitView, creditView),
                txn.getCreatedAt()
        );
    }

    private TransferResponse buildExistingResponse(LedgerTransaction existing) {
        List<LedgerEntry> entries = entryRepository.findByTransactionId(existing.getId());
        List<TransferResponse.LedgerEntryView> entryViews = entries.stream()
                .map(e -> new TransferResponse.LedgerEntryView(
                        e.getId(),
                        e.getAccount().getAccountNumber(),
                        e.getEntryType(),
                        e.getAmount(),
                        e.getCurrency()
                ))
                .toList();

        return new TransferResponse(
                existing.getId(),
                existing.getTransactionType(),
                existing.getStatus(),
                entryViews,
                existing.getCreatedAt()
        );
    }

    private String computePayloadHash(TransferRequest request) {
        String amountStr = request.amount() != null
                ? request.amount().stripTrailingZeros().toPlainString()
                : "";
        String raw = String.join(":",
                request.debitAccountNumber() != null ? request.debitAccountNumber() : "",
                request.creditAccountNumber() != null ? request.creditAccountNumber() : "",
                amountStr,
                request.currency() != null ? request.currency() : ""
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
