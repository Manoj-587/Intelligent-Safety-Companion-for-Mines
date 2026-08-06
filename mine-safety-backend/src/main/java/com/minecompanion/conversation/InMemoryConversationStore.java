package com.minecompanion.conversation;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap-backed implementation of ConversationMemoryStore.
 *
 * Session expiry is enforced by a scheduled task that runs every minute
 * and removes sessions whose lastActivity is older than session-expiration-ms.
 *
 * Replace this class with a Redis or JDBC implementation by:
 *   1. Implementing ConversationMemoryStore in the new class.
 *   2. Annotating it @Primary @Component.
 *   3. Removing or disabling this class — no other code changes needed.
 */
@Slf4j
@Component
public class InMemoryConversationStore implements ConversationMemoryStore {

    @Value("${conversation.session-expiration-ms}")
    private long sessionExpirationMs;

    private final ConcurrentHashMap<String, ConversationSession> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[ConversationStore] In-memory store initialised | expiration={}ms", sessionExpirationMs);
    }

    @Override
    public ConversationSession getOrCreate(String sessionId, int maxTurns) {
        return store.computeIfAbsent(sessionId, id -> {
            log.debug("[ConversationStore] Creating new session | sessionId={}", id);
            return new ConversationSession(id, maxTurns);
        });
    }

    @Override
    public Optional<ConversationSession> find(String sessionId) {
        return Optional.ofNullable(store.get(sessionId));
    }

    @Override
    public void save(ConversationSession session) {
        store.put(session.getSessionId(), session);
    }

    @Override
    public void remove(String sessionId) {
        store.remove(sessionId);
        log.debug("[ConversationStore] Session removed | sessionId={}", sessionId);
    }

    @Override
    public int size() {
        return store.size();
    }

    // ── Expiry sweep — runs every 60 seconds ───────────────────────────────────

    @Scheduled(fixedDelayString = "${conversation.expiry-sweep-ms:60000}")
    public void evictExpiredSessions() {
        Instant cutoff = Instant.now().minusMillis(sessionExpirationMs);
        int before = store.size();

        store.entrySet().removeIf(entry -> entry.getValue().getLastActivity().isBefore(cutoff));

        int removed = before - store.size();
        if (removed > 0) {
            log.info("[ConversationStore] Evicted {} expired session(s) | active={}", removed, store.size());
        }
    }
}
