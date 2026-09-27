package com.example.ledger.transaction;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for the double-entry ledger.
 *
 * <p>Phase 4 adds deposit and withdrawal using the system account.
 */
@RestController
@RequestMapping("/transactions")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * POST /ledger/transfer
     *
     * <p>Creates a balanced double-entry transaction:
     * <pre>
     *   DEBIT  debitAccountNumber  amount
     *   CREDIT creditAccountNumber amount
     * </pre>
     *
     * <p>Example request body:
     * <pre>
     * {
     *   "debitAccountNumber":  "ACC-001",
     *   "creditAccountNumber": "ACC-002",
     *   "amount":              "100.00",
     *   "currency":            "INR",
     *   "idempotencyKey":      "txn-abc-001"
     * }
     * </pre>
     *
     * @return 201 Created with the transaction and its ledger entry pair
     */
    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            @RequestBody TransferRequest request
    ) {
        String key = (idempotencyHeader != null && !idempotencyHeader.isBlank())
                ? idempotencyHeader
                : request.idempotencyKey();
        TransferRequest effectiveRequest = new TransferRequest(
                request.debitAccountNumber(),
                request.creditAccountNumber(),
                request.amount(),
                request.currency(),
                key
        );
        return ledgerService.postTransfer(effectiveRequest);
    }

    /**
     * POST /transactions/deposit
     */
    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse deposit(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            @RequestBody DepositRequest request
    ) {
        String key = (idempotencyHeader != null && !idempotencyHeader.isBlank())
                ? idempotencyHeader
                : request.idempotencyKey();
        DepositRequest effectiveRequest = new DepositRequest(
                request.accountNumber(),
                request.amount(),
                request.currency(),
                key
        );
        return ledgerService.postDeposit(effectiveRequest);
    }

    /**
     * POST /transactions/withdraw
     */
    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse withdraw(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            @RequestBody WithdrawRequest request
    ) {
        String key = (idempotencyHeader != null && !idempotencyHeader.isBlank())
                ? idempotencyHeader
                : request.idempotencyKey();
        WithdrawRequest effectiveRequest = new WithdrawRequest(
                request.accountNumber(),
                request.amount(),
                request.currency(),
                key
        );
        return ledgerService.postWithdraw(effectiveRequest);
    }
}
