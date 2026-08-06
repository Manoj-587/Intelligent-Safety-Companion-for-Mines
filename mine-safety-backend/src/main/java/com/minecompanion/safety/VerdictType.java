package com.minecompanion.safety;

/**
 * The four possible outcomes of a SafetyPolicy evaluation.
 *
 * PASS               — policy is satisfied; processing continues.
 * PASS_WITH_WARNING  — policy is satisfied with an active safety advisory (e.g. HIGH risk active).
 *                      The chat remains conversational (blocked = false).
 * BLOCK              — policy is violated; pre-approved reply is returned and chat is blocked.
 * ESCALATE           — policy detected a critical condition requiring human attention.
 */
public enum VerdictType {
    PASS,
    PASS_WITH_WARNING,
    BLOCK,
    ESCALATE
}
