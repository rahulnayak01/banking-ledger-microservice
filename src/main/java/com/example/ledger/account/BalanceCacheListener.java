package com.example.ledger.account;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BalanceCacheListener {

    private final CacheManager cacheManager;

    public BalanceCacheListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceChanged(BalanceChangedEvent event) {

        Cache cache = cacheManager.getCache("accountBalances");

        if (cache != null) {
            com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache =
                    (com.github.benmanes.caffeine.cache.Cache<?, ?>)
                            cache.getNativeCache();

            System.out.println("BEFORE: " + caffeineCache.asMap());

            System.out.println("EVICTING: " + event.accountId());

            cache.evict(event.accountId());

            System.out.println("AFTER: " + caffeineCache.asMap());
        }
    }
}
