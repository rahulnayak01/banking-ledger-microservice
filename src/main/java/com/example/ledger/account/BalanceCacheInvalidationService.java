package com.example.ledger.account;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BalanceCacheInvalidationService {

    private final ApplicationEventPublisher eventPublisher;

    public BalanceCacheInvalidationService(
            ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void evictAfterCommit(UUID accountId) {
        eventPublisher.publishEvent(
                new BalanceChangedEvent(accountId)
        );
    }
}