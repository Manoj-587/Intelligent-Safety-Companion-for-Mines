package com.minecompanion.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Centralized exception handler for all REST endpoints.
 *
 * Maps each CompanionException subtype to an appropriate HTTP status
 * and returns a consistent error body:
 *   { "error": "<message>", "timestamp": "<iso-instant>" }
 *
 * This keeps ChatController free of try/catch blocks and ensures
 * every error response has the same shape for the React client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── AIML unavailable → 503 Service Unavailable ────────────────────────────
    @ExceptionHandler(AimlUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAimlUnavailable(AimlUnavailableException ex) {
        log.error("[ExceptionHandler] AIML API unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // ── LLM failure → 502 Bad Gateway ─────────────────────────────────────────
    @ExceptionHandler(LlmException.class)
    public ResponseEntity<Map<String, String>> handleLlm(LlmException ex) {
        log.error("[ExceptionHandler] LLM call failed: {}", ex.getMessage());
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    // ── Knowledge base failure → 500 Internal Server Error ────────────────────
    @ExceptionHandler(KnowledgeException.class)
    public ResponseEntity<Map<String, String>> handleKnowledge(KnowledgeException ex) {
        log.error("[ExceptionHandler] Knowledge base error: {}", ex.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // ── Generic companion error → 500 ─────────────────────────────────────────
    @ExceptionHandler(CompanionException.class)
    public ResponseEntity<Map<String, String>> handleCompanion(CompanionException ex) {
        log.error("[ExceptionHandler] Companion error: {}", ex.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // ── Bean validation failure → 400 Bad Request ─────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        log.warn("[ExceptionHandler] Validation failed: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    // ── Catch-all → 500 ───────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("[ExceptionHandler] Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error",     message,
                "timestamp", Instant.now().toString()
        ));
    }
}
