package com.minecompanion.conversation;

import java.util.Optional;

/**
 * Port interface for the conversation session store.
 *
 * ConversationMemoryService depends on this interface only.
 * Swap the implementation (Redis, JDBC, etc.) by providing a new
 * @Primary @Component — no changes to ConversationMemoryService required.
 */
public interface ConversationMemoryStore {

    /**
     * Returns the session for the given id, creating it if it does not exist.
     *
     * @param sessionId  client-supplied session identifier
     * @param maxTurns   sliding window size; used only when creating a new session
     */
    ConversationSession getOrCreate(String sessionId, int maxTurns);

    /** Returns the session if it exists, empty otherwise. */
    Optional<ConversationSession> find(String sessionId);

    /** Persists or updates the session. No-op for in-memory; required for external stores. */
    void save(ConversationSession session);

    /** Removes the session. Called on explicit logout or expiry. */
    void remove(String sessionId);

    /** Returns the number of active sessions currently held by the store. */
    int size();
}
