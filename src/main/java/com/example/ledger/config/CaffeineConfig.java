package com.example.ledger.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CaffeineConfig {

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager("accountBalances");

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
        );

        return cacheManager;
    }
}