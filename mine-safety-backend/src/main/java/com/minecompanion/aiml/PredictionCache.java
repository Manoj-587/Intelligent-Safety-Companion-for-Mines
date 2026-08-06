package com.minecompanion.aiml;

import java.util.Optional;

/**
 * Extension point for prediction caching.
 *
 * AimlApiClient depends on this interface, not on any concrete implementation.
 * This allows the cache backend to be swapped (e.g. in-memory → Redis)
 * without modifying the client or any other class.
 *
 * Implementations:
 *   InMemoryPredictionCache  — current implementation (ConcurrentHashMap + TTL)
 *   RedisPredictionCache     — future implementation when horizontal scaling is needed
 */
public interface PredictionCache {

    /**
     * Returns a cached PredictionResult for the given cache key, if present and not expired.
     *
     * @param key SHA-256 hash of the sensor payload
     * @return cached result, or empty if absent or expired
     */
    Optional<PredictionResult> get(String key);

    /**
     * Stores a PredictionResult under the given cache key.
     *
     * @param key    SHA-256 hash of the sensor payload
     * @param result the prediction to cache
     */
    void put(String key, PredictionResult result);

    /**
     * Removes all expired entries. Called periodically to prevent unbounded growth.
     */
    void evictExpired();
}
