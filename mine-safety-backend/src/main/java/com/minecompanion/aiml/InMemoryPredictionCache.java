package com.minecompanion.aiml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PredictionCache.
 *
 * Uses a ConcurrentHashMap of cache entries, each carrying the result
 * and the expiry timestamp. Thread-safe for concurrent request handling.
 *
 * Expiration is checked on every get() call (lazy expiry) and also
 * cleaned up on a fixed schedule (eager eviction) to prevent unbounded growth.
 *
 * Replace this bean with a Redis implementation by:
 *   1. Creating RedisPredictionCache implements PredictionCache
 *   2. Annotating this class with @ConditionalOnProperty(name="aiml.api.cache.backend", havingValue="memory")
 *   3. Annotating the Redis class with @ConditionalOnProperty(name="aiml.api.cache.backend", havingValue="redis")
 * No changes to AimlApiClient are required.
 */
@Slf4j
@Component
@EnableScheduling
public class InMemoryPredictionCache implements PredictionCache {

    @Value("${aiml.api.cache.expiration-ms}")
    private long expirationMs;

    @Value("${aiml.api.cache.enabled}")
    private boolean cacheEnabled;

    private record CacheEntry(PredictionResult result, Instant expiresAt) {}

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<PredictionResult> get(String key) {
        if (!cacheEnabled) return Optional.empty();

        CacheEntry entry = store.get(key);
        if (entry == null) return Optional.empty();

        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            log.debug("[PredictionCache] Cache entry expired and removed | key={}", key);
            return Optional.empty();
        }

        log.debug("[PredictionCache] Cache hit | key={}", key);
        return Optional.of(entry.result());
    }

    @Override
    public void put(String key, PredictionResult result) {
        if (!cacheEnabled) return;

        store.put(key, new CacheEntry(result, Instant.now().plusMillis(expirationMs)));
        log.debug("[PredictionCache] Cached prediction | key={} ttl={}ms", key, expirationMs);
    }

    @Override
    @Scheduled(fixedDelayString = "${aiml.api.cache.expiration-ms}")
    public void evictExpired() {
        Instant now = Instant.now();
        int before = store.size();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("[PredictionCache] Evicted {} expired entries | remaining={}", removed, store.size());
        }
    }
}
