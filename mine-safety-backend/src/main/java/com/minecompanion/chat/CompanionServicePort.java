package com.minecompanion.chat;

/**
 * Port interface for the AI Safety Companion orchestration service.
 *
 * ChatController depends on this interface, not on CompanionService directly.
 * This allows:
 *   - ChatController to be unit-tested with a mock implementation.
 *   - CompanionService to be swapped without touching the controller.
 *
 * Implemented by CompanionService in Step 10.
 */
public interface CompanionServicePort {

    /**
     * Processes a user chat request through the full companion pipeline:
     * Intent Classification → Safety Guard → Knowledge Retrieval →
     * AIML API Call → Prompt Building → LLM Call → Response Generation.
     *
     * @param request the validated inbound chat request
     * @return the AI-generated or pre-approved safety response
     */
    ChatResponse process(ChatRequest request);
}
