package com.example.ledger.ledger;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for executing ledger replay operations and rebuilding balance projections.
 */
@RestController
@RequestMapping("/ledger")
public class LedgerReplayController {

    private final LedgerReplayService ledgerReplayService;

    public LedgerReplayController(LedgerReplayService ledgerReplayService) {
        this.ledgerReplayService = ledgerReplayService;
    }

    /**
     * POST or GET /ledger/rebuild-balance/{accountNumber}
     *
     * <p>Replays the ledger history for a single account and updates its balance projection.
     */
    @RequestMapping(value = "/rebuild-balance/{accountNumber}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseStatus(HttpStatus.OK)
    public RebuildBalanceResponse rebuildBalanceByAccountNumber(
            @PathVariable String accountNumber
    ) {
        return ledgerReplayService.rebuildBalanceByAccountNumber(accountNumber);
    }

    /**
     * POST or GET /ledger/rebuild-balance/id/{accountId}
     *
     * <p>Replays the ledger history for a single account by UUID.
     */
    @RequestMapping(value = "/rebuild-balance/id/{accountId}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseStatus(HttpStatus.OK)
    public RebuildBalanceResponse rebuildBalanceById(
            @PathVariable UUID accountId
    ) {
        return ledgerReplayService.rebuildBalance(accountId);
    }

    /**
     * POST or GET /ledger/rebuild-balances
     *
     * <p>Replays the entire ledger history across all accounts and restores all balance projections.
     */
    @RequestMapping(value = "/rebuild-balances", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseStatus(HttpStatus.OK)
    public RebuildAllBalancesResponse rebuildAllBalances() {
        return ledgerReplayService.rebuildAllBalances();
    }
}
