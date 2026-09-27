package com.example.ledger.account;

import com.example.ledger.customer.Customer;
import com.example.ledger.customer.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the BANK-CASH system account exists on application startup.
 *
 * <p><b>Why do we need a system account?</b>
 *
 * <p>In double-entry accounting, money cannot appear from nowhere.
 * When a customer deposits ₹100, we need <em>two</em> legs:
 * <pre>
 *   DEBIT  BANK-CASH   ₹100   (bank's cash pool increases)
 *   CREDIT CUSTOMER-A  ₹100   (customer's balance increases)
 * </pre>
 *
 * <p>Without the system account, we'd only have one entry and the
 * ledger would be unbalanced — violating the fundamental accounting equation.
 *
 * <p>The system account is <b>not</b> a real customer. It represents the
 * bank's internal cash position, similar to a "suspense" or "nostro" account
 * in real banking systems.
 *
 * <p>This component runs at startup via {@link CommandLineRunner} and is
 * idempotent — if the account already exists, it skips creation.
 */
@Component
public class SystemAccountInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SystemAccountInitializer.class);

    /**
     * Well-known constants for the system account.
     * These are used by LedgerService to look up the system account.
     */
    public static final String SYSTEM_CUSTOMER_ID = "SYSTEM";
    public static final String BANK_CASH_ACCOUNT_NUMBER = "BANK-CASH-INR";
    public static final String BANK_CASH_CURRENCY = "INR";

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    public SystemAccountInitializer(CustomerRepository customerRepository,
                                    AccountRepository accountRepository,
                                    AccountBalanceRepository accountBalanceRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // --- Idempotent check ---
        if (accountRepository.findByAccountNumber(BANK_CASH_ACCOUNT_NUMBER)
                .isPresent()) {
            log.info("System account {} already exists — skipping init",
                    BANK_CASH_ACCOUNT_NUMBER);
            return;
        }

        // --- Create "SYSTEM" customer if needed ---
        Customer systemCustomer = customerRepository
                .findByExternalCustomerId(SYSTEM_CUSTOMER_ID)
                .orElseGet(() -> {
                    log.info("Creating SYSTEM customer");
                    return customerRepository.save(
                            new Customer(SYSTEM_CUSTOMER_ID)
                    );
                });

        // --- Create the BANK-CASH account ---
        Account bankCash = new Account(
                systemCustomer,
                BANK_CASH_ACCOUNT_NUMBER,
                BANK_CASH_CURRENCY,
                AccountType.SYSTEM
        );
        bankCash = accountRepository.save(bankCash);

        // --- Create its balance record ---
        accountBalanceRepository.save(new AccountBalance(bankCash.getId()));

        log.info("Created system account: {} (id={})",
                BANK_CASH_ACCOUNT_NUMBER, bankCash.getId());
    }
}
