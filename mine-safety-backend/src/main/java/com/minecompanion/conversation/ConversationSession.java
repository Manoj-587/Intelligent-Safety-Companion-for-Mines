package com.minecompanion.conversation;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Holds all conversational state for one session.
 *
 * window       — sliding window of the most recent N turns (N = max-history-turns).
 * summary      — rolling text summary of turns that have been evicted from the window.
 * currentTopic — the last classified intent label; used by IntentClassifier for
 *                context enrichment of vague follow-up messages.
 * lastActivity — updated on every interaction; used by the store for expiry checks.
 *
 * This class is not thread-safe on its own. ConversationMemoryService synchronises
 * access per sessionId using ConcurrentHashMap + per-session locking.
 */
@Getter
public class ConversationSession {

    private final String          sessionId;
    private final Deque<ConversationTurn> window;
    private final int             maxTurns;

    @Setter private String  summary       = "";
    @Setter private String  currentTopic  = "";
    @Setter private Instant lastActivity  = Instant.now();

    public ConversationSession(String sessionId, int maxTurns) {
        this.sessionId = sessionId;
        this.maxTurns  = maxTurns;
        this.window    = new ArrayDeque<>(maxTurns + 1);
    }

    /**
     * Adds a completed turn to the window.
     * If the window is full, the oldest turn is evicted and returned
     * so the caller can fold it into the rolling summary.
     *
     * @return the evicted turn, or null if no eviction occurred
     */
    public ConversationTurn addTurn(ConversationTurn turn) {
        ConversationTurn evicted = null;
        if (window.size() >= maxTurns) {
            evicted = window.pollFirst();
        }
        window.addLast(turn);
        lastActivity = Instant.now();
        return evicted;
    }

    /** Returns the turns in the window as an ordered list (oldest first). */
    public List<ConversationTurn> getWindowAsList() {
        return new ArrayList<>(window);
    }

    /** Returns the last AI reply in the window, or null if the window is empty. */
    public String getLastAiReply() {
        ConversationTurn last = window.peekLast();
        return last != null ? last.aiReply() : null;
    }

    public boolean isEmpty() {
        return window.isEmpty();
    }
}
