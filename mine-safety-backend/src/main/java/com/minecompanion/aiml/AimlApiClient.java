package com.minecompanion.aiml;

import com.fasterxml.jackson.databind.JsonNode;
import com.minecompanion.exception.AimlUnavailableException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the AIML Flask prediction API.
 *
 * Responsibilities:
 *   1. Startup health check  — warns if Flask is unreachable at boot time.
 *   2. Cache-first lookup    — returns a cached PredictionResult for identical payloads.
 *   3. Retry on transient failures — retries up to max-attempts for network/timeout errors.
 *                                    Never retries 4xx client/validation errors.
 *   4. Stage logging         — logs the AIML API Call stage of the pipeline.
 *
 * The Flask API contract is never modified:
 *   GET  /         → health check
 *   POST /predict  → { predicted_risk, reasons, recommendations }
 */
@Slf4j
@Service
public class AimlApiClient {

    private final WebClient        webClient;
    private final PredictionCache  cache;

    @Value("${aiml.api.predict-endpoint}")
    private String predictEndpoint;

    @Value("${aiml.api.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${aiml.api.retry.delay-ms}")
    private long retryDelayMs;

    public AimlApiClient(@Qualifier("aimlWebClient") WebClient webClient,
                         PredictionCache cache) {
        this.webClient = webClient;
        this.cache     = cache;
    }

    // ── Startup Health Check ───────────────────────────────────────────────────

    /**
     * Called once after the bean is constructed.
     * Pings GET / on the Flask API to verify it is reachable.
     * Logs a warning if unavailable — does not prevent Spring Boot from starting,
     * because the Flask service may start after Spring Boot in some deployments.
     */
    @PostConstruct
    public void checkHealth() {
        log.info("[AimlApiClient] Performing startup health check on AIML Flask API...");
        try {
            String response = webClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("[AimlApiClient] AIML Flask API is reachable. Response: {}", response);
        } catch (Exception ex) {
            log.warn("[AimlApiClient] AIML Flask API is NOT reachable at startup. " +
                     "Predictions will fail until the service is available. Cause: {}", ex.getMessage());
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Calls POST /predict on the Flask API with the given sensor payload.
     *
     * Flow:
     *   1. Compute cache key from payload hash.
     *   2. Return cached result if present and not expired.
     *   3. Call Flask API with retry on transient failures.
     *   4. Cache and return the result.
     *
     * @param sensorPayload map of 27 sensor feature names to their numeric values
     * @return PredictionResult containing predicted_risk, reasons, recommendations
     * @throws AimlUnavailableException if the Flask API is unreachable after all retries,
     *                                  or returns a 5xx error
     */
    public PredictionResult predict(Map<String, Object> sensorPayload) {

        // ── Stage: AIML API Call ───────────────────────────────────────────────
        log.info("[AimlApiClient] ▶ AIML API Call | features={}", sensorPayload.size());

        // Step 1: Cache lookup
        String cacheKey = computeHash(sensorPayload);
        Optional<PredictionResult> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            log.info("[AimlApiClient] Cache hit — returning cached prediction | risk={}",
                    cached.get().getPredictedRisk());
            return cached.get();
        }

        // Step 2: Call Flask with retry
        PredictionResult result = callWithRetry(sensorPayload, cacheKey);

        // Step 3: Cache the result
        cache.put(cacheKey, result);

        log.info("[AimlApiClient] ◀ AIML API Call complete | risk={} reasons={} recommendations={}",
                result.getPredictedRisk(),
                result.getReasons().size(),
                result.getRecommendations().size());

        return result;
    }

    // ── Retry Logic ────────────────────────────────────────────────────────────

    /**
     * Executes the Flask /predict call with up to maxRetryAttempts retries.
     *
     * Retry policy:
     *   - Retries on: WebClientRequestException (network error, timeout)
     *   - Retries on: 5xx server errors from Flask
     *   - Does NOT retry: 4xx client/validation errors — these indicate a bad
     *     payload that will fail on every attempt.
     */
    private PredictionResult callWithRetry(Map<String, Object> payload, String cacheKey) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return callFlask(payload);
            } catch (AimlUnavailableException ex) {
                // 4xx errors are wrapped with a non-retryable flag — rethrow immediately
                if (ex.getMessage().startsWith("[4xx]")) {
                    log.warn("[AimlApiClient] Non-retryable client error from Flask: {}", ex.getMessage());
                    throw ex;
                }
                if (attempt > maxRetryAttempts) {
                    log.error("[AimlApiClient] All {} attempt(s) exhausted. Flask API unavailable.",
                            maxRetryAttempts + 1);
                    throw ex;
                }
                log.warn("[AimlApiClient] Transient failure on attempt {}/{}. Retrying in {}ms. Cause: {}",
                        attempt, maxRetryAttempts + 1, retryDelayMs, ex.getMessage());
                sleep(retryDelayMs);
            }
        }
    }

    // ── Flask HTTP Call ────────────────────────────────────────────────────────

    private PredictionResult callFlask(Map<String, Object> payload) {
        try {
            JsonNode body = webClient.post()
                    .uri(predictEndpoint)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class).map(err ->
                                    new AimlUnavailableException("[4xx] Flask validation error: " + err)))
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class).map(err ->
                                    new AimlUnavailableException("Flask server error: " + err)))
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseResponse(body);

        } catch (AimlUnavailableException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw new AimlUnavailableException("Network error reaching AIML Flask API: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new AimlUnavailableException("Unexpected error calling AIML Flask API: " + ex.getMessage(), ex);
        }
    }

    // ── Response Parsing ───────────────────────────────────────────────────────

    /**
     * Parses the Flask JSON response into a PredictionResult.
     * Field names match the Flask schema: predicted_risk, reasons, recommendations.
     */
    private PredictionResult parseResponse(JsonNode body) {
        if (body == null) {
            throw new AimlUnavailableException("Flask API returned an empty response body.");
        }

        String predictedRisk = body.path("predicted_risk").asText(null);
        if (predictedRisk == null || predictedRisk.isBlank()) {
            throw new AimlUnavailableException("Flask response missing 'predicted_risk' field.");
        }

        List<String> reasons = parseStringArray(body, "reasons");
        List<String> recommendations = parseStringArray(body, "recommendations");

        return PredictionResult.builder()
                .predictedRisk(predictedRisk)
                .reasons(reasons)
                .recommendations(recommendations)
                .build();
    }

    private List<String> parseStringArray(JsonNode body, String field) {
        JsonNode node = body.path(field);
        if (node.isMissingNode() || !node.isArray()) return List.of();
        List<String> result = new java.util.ArrayList<>();
        node.forEach(n -> result.add(n.asText()));
        return List.copyOf(result);
    }

    // ── Cache Key ──────────────────────────────────────────────────────────────

    /**
     * Computes a SHA-256 hash of the sensor payload's string representation.
     * Used as the cache key — identical payloads always produce the same key.
     */
    private String computeHash(Map<String, Object> payload) {
        try {
            // Sort by key for deterministic ordering before hashing
            String canonical = new java.util.TreeMap<>(payload).toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed to be available in all JVMs
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
