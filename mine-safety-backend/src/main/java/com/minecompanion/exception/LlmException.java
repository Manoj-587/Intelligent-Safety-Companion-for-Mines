package com.minecompanion.exception;

/**
 * Thrown when the LLM provider call fails, times out, or returns
 * an unparseable response.
 *
 * GlobalExceptionHandler maps this to a 502 Bad Gateway so the
 * React client can display a meaningful fallback message.
 */
public class LlmException extends CompanionException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
