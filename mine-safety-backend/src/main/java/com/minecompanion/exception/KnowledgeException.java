package com.minecompanion.exception;

/**
 * Thrown when the knowledge base fails to load its source files
 * at startup, or when a retrieval query cannot be completed.
 *
 * SafetyGuard intercepts this for EMERGENCY_GUIDANCE intents
 * and returns a pre-approved fallback response instead of
 * allowing the LLM to answer without grounded knowledge.
 */
public class KnowledgeException extends CompanionException {

    public KnowledgeException(String message) {
        super(message);
    }

    public KnowledgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
