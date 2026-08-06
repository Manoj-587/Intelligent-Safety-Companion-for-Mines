package com.minecompanion.exception;

/**
 * Base exception for all AI Safety Companion domain errors.
 * All module-specific exceptions extend this class so CompanionService
 * and GlobalExceptionHandler can catch them uniformly.
 */
public class CompanionException extends RuntimeException {

    public CompanionException(String message) {
        super(message);
    }

    public CompanionException(String message, Throwable cause) {
        super(message, cause);
    }
}
