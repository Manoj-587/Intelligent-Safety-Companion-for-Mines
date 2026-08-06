package com.minecompanion.chat;

/**
 * Represents the role of the user interacting with the AI Safety Companion.
 * PromptBuilder uses this to adjust language complexity and response depth.
 *
 * WORKER           — simple, direct language; immediate action focus.
 * SUPERVISOR       — operational summary; moderate technical depth.
 * MAINTENANCE      — technical diagnosis; equipment-level detail.
 * SAFETY_OFFICER   — full technical explanation; regulatory context.
 */
public enum UserRole {
    WORKER,
    SUPERVISOR,
    MAINTENANCE,
    SAFETY_OFFICER
}
