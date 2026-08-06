package com.minecompanion.conversation;

import java.time.Instant;

/**
 * One completed conversation turn: the user's message and the AI's reply.
 * Stored in ConversationSession's sliding window. Immutable after creation.
 */
public record ConversationTurn(
        String  userMessage,
        String  aiReply,
        Instant timestamp
) {
    public static ConversationTurn of(String userMessage, String aiReply) {
        return new ConversationTurn(userMessage, aiReply, Instant.now());
    }

    /** Formats this turn as two labelled lines for prompt injection. */
    public String toPromptLines() {
        return "User: " + userMessage + "\nAssistant: " + aiReply;
    }
}
