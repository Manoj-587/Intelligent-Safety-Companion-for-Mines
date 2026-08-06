package com.minecompanion.companion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classifies a user message into a CompanionIntent.
 *
 * Classification strategy:
 *   1. Normalise the message (lowercase, trim).
 *   2. Evaluate every IntentRule against the normalised message.
 *   3. Select the rule with the highest confidence score.
 *   4. If no rule matches, return UNKNOWN.
 *
 * Extensibility:
 *   Adding a new intent = adding one IntentRule to RULES.
 *   CompanionService reads CompanionIntent values directly — no routing
 *   changes are needed when new intents are added.
 *
 * Conversation context:
 *   The classifier accepts the last AI reply as context so it can resolve
 *   pronouns and references (e.g. "What should I do now?" after a methane answer
 *   is classified as EMERGENCY_GUIDANCE, not UNKNOWN).
 */
@Slf4j
@Service
public class IntentClassifier {

    // ── Rule table ─────────────────────────────────────────────────────────────
    // Rules are evaluated in list order; the highest-confidence match wins.
    // Confidence values reflect how specific the pattern set is:
    //   0.95 — very specific phrases with little ambiguity
    //   0.85 — clear domain phrases
    //   0.75 — broader terms that could appear in multiple contexts
    //   0.60 — weak signals used only as tiebreakers

    private static final List<IntentRule> RULES = List.of(

        // ── EMERGENCY_GUIDANCE ─────────────────────────────────────────────────
        new IntentRule(CompanionIntent.EMERGENCY_GUIDANCE, Set.of(
            "fire", "smoke", "explosion", "blast", "collapse", "roof fall",
            "trapped", "evacuate", "evacuation", "emergency", "alarm",
            "methane leak", "gas leak", "outburst", "inrush", "flood",
            "someone is injured", "person down", "unconscious"
        ), 0.95),

        // ── RISK_EXPLANATION ───────────────────────────────────────────────────
        new IntentRule(CompanionIntent.RISK_EXPLANATION, Set.of(
            "why is the risk", "why is risk", "why high risk", "why medium risk",
            "why low risk", "explain the risk", "what caused the risk",
            "risk level", "predicted risk", "current risk", "risk today",
            "why is today", "what does high mean", "what does medium mean"
        ), 0.95),

        // ── RECOMMENDATION_EXPLANATION ─────────────────────────────────────────
        new IntentRule(CompanionIntent.RECOMMENDATION_EXPLANATION, Set.of(
            "why should i", "why stop", "why isolate", "why reduce",
            "explain the recommendation", "what does this recommendation",
            "why is this recommended", "why do i need to", "reason for recommendation",
            "what does immediate mean", "what does inspection mean"
        ), 0.90),

        // ── FIRST_AID ──────────────────────────────────────────────────────────
        new IntentRule(CompanionIntent.FIRST_AID, Set.of(
            "first aid", "injured", "unconscious", "not breathing", "cpr",
            "rescue breathing", "treatment", "symptoms", "poisoning",
            "gas exposure", "co poisoning", "h2s exposure", "recovery position",
            "bleeding", "burn", "fracture", "medical"
        ), 0.90),

        // ── PPE_GUIDANCE ───────────────────────────────────────────────────────
        new IntentRule(CompanionIntent.PPE_GUIDANCE, Set.of(
            "ppe", "personal protective", "hard hat", "helmet", "safety boots",
            "gloves", "respirator", "self-rescuer", "cap lamp", "hearing protection",
            "what should i wear", "what to wear", "protective equipment",
            "safety gear", "high visibility", "reflective"
        ), 0.90),

        // ── EQUIPMENT_HELP ─────────────────────────────────────────────────────
        new IntentRule(CompanionIntent.EQUIPMENT_HELP, Set.of(
            "crusher", "conveyor", "pump", "motor", "bearing", "electrical panel",
            "winder", "fan", "compressor", "drill", "equipment fault",
            "machine fault", "equipment failure", "overheating equipment",
            "vibration", "noise from", "strange noise", "equipment stopped",
            "cr863", "tc862", "wm868", "cm861", "p_864"
        ), 0.85),

        // ── SAFETY_QUESTION ────────────────────────────────────────────────────
        new IntentRule(CompanionIntent.SAFETY_QUESTION, Set.of(
            "is it safe", "safe to", "safety rule", "safety procedure",
            "safety regulation", "dgms", "safety standard", "hazard",
            "risk assessment", "safe working", "safety check", "safety inspection",
            "what is the limit", "permissible limit", "threshold"
        ), 0.85),

        // ── GENERAL_MINING ─────────────────────────────────────────────────────
        new IntentRule(CompanionIntent.GENERAL_MINING, Set.of(
            "what is methane", "what is co", "what is ventilation",
            "explain ventilation", "explain methane", "explain gas",
            "what is a goaf", "what is a stope", "what is firedamp",
            "what is blackdamp", "what is whitedamp", "mining term",
            "what does", "explain", "how does", "what is", "tell me about",
            "ventilation", "airflow", "gas concentration", "barometric"
        ), 0.75),

        // ── SMALL_TALK ─────────────────────────────────────────────────────────
        new IntentRule(CompanionIntent.SMALL_TALK, Set.of(
            "hello", "hi", "hey", "good morning", "good afternoon", "good evening",
            "how are you", "what can you do", "who are you", "what are you",
            "thank you", "thanks", "ok", "okay", "got it", "understood"
        ), 0.80)
    );

    // ── Stop words excluded from keyword extraction ────────────────────────────
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "the", "is", "it", "in", "on", "at", "to", "for",
        "of", "and", "or", "but", "not", "with", "this", "that", "what",
        "why", "how", "do", "i", "me", "my", "we", "you", "your", "be",
        "are", "was", "were", "has", "have", "had", "will", "would", "can",
        "could", "should", "tell", "about", "please", "now", "just"
    );

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Classifies the user message into a CompanionIntent.
     *
     * @param message         the raw user message
     * @param lastAiReply     the previous AI response (used for context resolution); may be null
     * @return ClassificationResult containing the intent, confidence, and extracted keywords
     */
    public ClassificationResult classify(String message, String lastAiReply) {

        // ── Stage: Intent Classification ──────────────────────────────────────
        log.info("[IntentClassifier] ▶ Intent Classification | message=\"{}\"",
                truncate(message, 100));

        String normalised = normalise(message);

        // Resolve contextual references using the last AI reply
        String contextEnriched = enrichWithContext(normalised, lastAiReply);

        // Evaluate all rules and pick the highest-confidence match
        IntentRule best = null;
        for (IntentRule rule : RULES) {
            if (rule.matches(contextEnriched)) {
                if (best == null || rule.confidence() > best.confidence()) {
                    best = rule;
                }
            }
        }

        CompanionIntent intent     = best != null ? best.intent() : CompanionIntent.UNKNOWN;
        double          confidence = best != null ? best.confidence() : 0.0;
        List<String>    keywords   = extractKeywords(normalised);

        log.info("[IntentClassifier] ◀ Intent Classification complete | intent={} confidence={} keywords={}",
                intent, confidence, keywords);

        return new ClassificationResult(intent, confidence, keywords);
    }

    // ── Context Resolution ─────────────────────────────────────────────────────

    /**
     * Enriches the normalised message with topic signals from the last AI reply.
     *
     * Example:
     *   User: "What should I do now?"
     *   Last reply contained "methane" → append "methane emergency" to the message
     *   → EMERGENCY_GUIDANCE is now matched instead of UNKNOWN.
     */
    private String enrichWithContext(String normalised, String lastAiReply) {
        if (lastAiReply == null || lastAiReply.isBlank()) return normalised;

        // Only enrich vague messages that lack strong intent signals on their own
        boolean isVague = normalised.split("\\s+").length <= 6;
        if (!isVague) return normalised;

        String lowerReply = lastAiReply.toLowerCase();
        StringBuilder enriched = new StringBuilder(normalised);

        if (lowerReply.contains("methane") || lowerReply.contains("gas leak"))  enriched.append(" methane gas");
        if (lowerReply.contains("fire") || lowerReply.contains("smoke"))        enriched.append(" fire emergency");
        if (lowerReply.contains("collapse") || lowerReply.contains("roof fall")) enriched.append(" collapse emergency");
        if (lowerReply.contains("high risk") || lowerReply.contains("risk level")) enriched.append(" risk level");
        if (lowerReply.contains("recommendation"))                               enriched.append(" recommendation");
        if (lowerReply.contains("bearing") || lowerReply.contains("conveyor"))  enriched.append(" equipment");
        if (lowerReply.contains("ppe") || lowerReply.contains("protective"))    enriched.append(" ppe");

        return enriched.toString();
    }

    // ── Keyword Extraction ─────────────────────────────────────────────────────

    /**
     * Extracts meaningful keywords from the normalised message by removing
     * stop words and short tokens. Used to drive KnowledgeBaseService tag matching.
     */
    private List<String> extractKeywords(String normalised) {
        return Arrays.stream(normalised.split("\\s+"))
                .map(w -> w.replaceAll("[^a-z0-9_]", ""))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String normalise(String message) {
        return message == null ? "" : message.toLowerCase().trim();
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    // ── Result record ──────────────────────────────────────────────────────────

    /**
     * Immutable result of a classification operation.
     *
     * @param intent     the classified CompanionIntent
     * @param confidence confidence score of the winning rule (0.0 – 1.0)
     * @param keywords   meaningful keywords extracted from the user message
     */
    public record ClassificationResult(
            CompanionIntent intent,
            double          confidence,
            List<String>    keywords
    ) {}
}
