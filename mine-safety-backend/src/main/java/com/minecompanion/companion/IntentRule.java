package com.minecompanion.companion;

import java.util.Set;

/**
 * An immutable rule that maps a set of trigger patterns to a CompanionIntent.
 *
 * IntentClassifier holds an ordered list of IntentRule instances.
 * Classification evaluates every rule against the normalised user message
 * and selects the highest-confidence match.
 *
 * Adding a new intent requires only a new IntentRule entry in IntentClassifier.
 * No changes to this class or to CompanionService are ever needed.
 *
 * @param intent     the intent this rule fires for
 * @param patterns   lowercase substrings or phrases that trigger this rule
 * @param confidence score assigned when this rule matches (0.0 – 1.0);
 *                   higher confidence rules win when multiple rules match
 */
public record IntentRule(CompanionIntent intent, Set<String> patterns, double confidence) {

    /**
     * Returns true if any pattern in this rule is found in the normalised message.
     * Matching is case-insensitive substring matching — no regex required.
     *
     * @param normalisedMessage lowercase, trimmed user message
     */
    public boolean matches(String normalisedMessage) {
        return patterns.stream().anyMatch(normalisedMessage::contains);
    }
}
