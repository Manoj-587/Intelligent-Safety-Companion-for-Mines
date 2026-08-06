package com.minecompanion.conversation;

import com.minecompanion.companion.CompanionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages per-session conversation state independently of the LLM provider.
 *
 * Responsibilities:
 *   - Maintain a sliding window of recent turns per session.
 *   - Generate a rolling text summary when turns are evicted from the window.
 *   - Track the current discussion topic (last classified intent label).
 *   - Expose only the data PromptBuilder needs via ConversationContext.
 *
 * All LLM-provider concerns are absent from this class.
 * The summary is built from structured turn data — no LLM call is made.
 *
 * Thread safety: ConcurrentHashMap in the store handles concurrent session
 * creation. Individual session mutations are serialised per sessionId via
 * synchronised blocks on the session object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final ConversationMemoryStore store;

    @Value("${conversation.max-history-turns}")
    private int maxHistoryTurns;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns the conversation context for a session.
     * Creates the session if it does not yet exist.
     * Called by CompanionService before building the prompt.
     *
     * @param sessionId the client session identifier
     * @return ConversationContext ready for PromptBuilder injection
     */
    public ConversationContext getContext(String sessionId) {
        ConversationSession session = store.getOrCreate(sessionId, maxHistoryTurns);
        return buildContext(session);
    }

    /**
     * Records a completed turn and updates topic tracking.
     * Called by CompanionService after the LLM reply is finalised.
     *
     * @param sessionId    the client session identifier
     * @param userMessage  the user's raw message
     * @param aiReply      the AI's reply text
     * @param intent       the classified intent for this turn
     */
    public void recordTurn(String sessionId, String userMessage, String aiReply, CompanionIntent intent) {
        ConversationSession session = store.getOrCreate(sessionId, maxHistoryTurns);

        synchronized (session) {
            ConversationTurn turn    = ConversationTurn.of(userMessage, aiReply);
            ConversationTurn evicted = session.addTurn(turn);

            // Update rolling summary with the evicted turn
            if (evicted != null) {
                session.setSummary(appendToSummary(session.getSummary(), evicted));
                log.debug("[ConversationMemory] Turn evicted from window | sessionId={}", sessionId);
            }

            // Track current topic from the intent label
            if (intent != null && intent != CompanionIntent.SMALL_TALK && intent != CompanionIntent.UNKNOWN) {
                session.setCurrentTopic(intent.getLabel());
            }

            store.save(session);
        }

        log.debug("[ConversationMemory] Turn recorded | sessionId={} intent={} windowSize={}",
                sessionId, intent, store.find(sessionId).map(s -> s.getWindowAsList().size()).orElse(0));
    }

    /**
     * Returns the last AI reply for a session.
     * Used by IntentClassifier for context enrichment of vague follow-up messages.
     *
     * @param sessionId the client session identifier
     * @return the last AI reply text, or null if the session has no history
     */
    public String getLastAiReply(String sessionId) {
        return store.find(sessionId)
                .map(ConversationSession::getLastAiReply)
                .orElse(null);
    }

    /**
     * Clears all history for a session.
     * Called on explicit logout or session reset.
     */
    public void clearSession(String sessionId) {
        store.remove(sessionId);
        log.info("[ConversationMemory] Session cleared | sessionId={}", sessionId);
    }

    // ── Context assembly ───────────────────────────────────────────────────────

    private ConversationContext buildContext(ConversationSession session) {
        List<String> turnLines = session.getWindowAsList().stream()
                .map(ConversationTurn::toPromptLines)
                .collect(Collectors.toList());

        return new ConversationContext(
                session.getSessionId(),
                turnLines,
                session.getSummary(),
                session.getCurrentTopic(),
                session.getWindowAsList().size()
        );
    }

    // ── Rolling summary ────────────────────────────────────────────────────────

    /**
     * Appends an evicted turn to the rolling summary.
     *
     * The summary is a compact, structured text block — not an LLM-generated
     * paragraph. This keeps the implementation LLM-provider-agnostic.
     *
     * Format:
     *   [Earlier in this conversation]
     *   User asked about: <first 120 chars of user message>
     *   Assistant addressed: <first 120 chars of AI reply>
     */
    private String appendToSummary(String existing, ConversationTurn evicted) {
        String header = existing.isBlank() ? "[Earlier in this conversation]\n" : existing + "\n";
        return header
                + "User asked about: "    + truncate(evicted.userMessage(), 120) + "\n"
                + "Assistant addressed: " + truncate(evicted.aiReply(),     120) + "\n";
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
