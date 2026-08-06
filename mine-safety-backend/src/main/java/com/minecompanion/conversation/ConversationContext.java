package com.minecompanion.conversation;

import java.util.List;

/**
 * Read-only view of a session's conversation state, exposed to PromptBuilder.
 *
 * Decouples PromptBuilder from ConversationSession internals.
 * ConversationMemoryService is the only class that creates instances.
 *
 * @param sessionId      the session this context belongs to
 * @param recentTurns    formatted turn lines from the sliding window (oldest first)
 * @param summary        rolling summary of turns evicted from the window; may be blank
 * @param currentTopic   label of the last non-trivial intent; may be blank
 * @param windowSize     number of turns currently in the sliding window
 */
public record ConversationContext(
        String       sessionId,
        List<String> recentTurns,
        String       summary,
        String       currentTopic,
        int          windowSize
) {
    /**
     * Returns the full conversation block for prompt injection:
     * summary (if any) followed by the recent window turns.
     */
    public String toPromptBlock() {
        if (recentTurns.isEmpty() && summary.isBlank()) return "";

        StringBuilder sb = new StringBuilder();
        if (!summary.isBlank()) {
            sb.append(summary).append("\n");
        }
        if (!recentTurns.isEmpty()) {
            sb.append("[Recent conversation]\n");
            recentTurns.forEach(line -> sb.append(line).append("\n"));
        }
        return sb.toString().stripTrailing();
    }
}
