package com.minecompanion.exception;

/**
 * Thrown when the AIML Flask prediction API is unreachable,
 * times out, or returns an unexpected error response.
 *
 * SafetyGuard intercepts this to block LLM calls that would
 * otherwise hallucinate sensor values.
 */
public class AimlUnavailableException extends CompanionException {

    public AimlUnavailableException(String message) {
        super(message);
    }

    public AimlUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
