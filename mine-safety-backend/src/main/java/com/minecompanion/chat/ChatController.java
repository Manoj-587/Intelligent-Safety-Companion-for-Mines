package com.minecompanion.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST entry point for the AI Safety Companion.
 *
 * Single endpoint: POST /api/chat
 *
 * All orchestration is delegated to CompanionService — this class
 * is intentionally thin. It handles only HTTP concerns:
 * request validation, response wrapping, and pipeline stage logging.
 *
 * Exception handling is fully delegated to GlobalExceptionHandler.
 * No try/catch blocks belong here.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    // CompanionService is injected here once it is implemented in Step 10.
    // Declared as an interface type so the implementation can be swapped.
    private final CompanionServicePort companionService;

    /**
     * Accepts a user message and returns an AI-generated safety response.
     *
     * Pipeline stages logged here (high-level):
     *   1. User Question received
     *   2. Delegated to CompanionService (which logs remaining stages internally)
     *   3. Response returned
     *
     * Detailed stage logging (Intent Classification → Knowledge Retrieval →
     * AIML API Call → LLM Call → Response Generation) is done inside each
     * respective service class using @Slf4j.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {

        // ── Stage 1: User Question ─────────────────────────────────────────────
        log.info("[Chat] ▶ User Question | session={} role={} message=\"{}\"",
                request.getSessionId(),
                request.getRole(),
                truncate(request.getMessage(), 120));

        ChatResponse response = companionService.process(request);

        // ── Stage 6: Response Generation ──────────────────────────────────────
        log.info("[Chat] ◀ Response Generated | session={} intent={} blocked={} sources={}",
                request.getSessionId(),
                response.getIntent(),
                response.isBlocked(),
                response.getSources());

        return ResponseEntity.ok(response);
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
