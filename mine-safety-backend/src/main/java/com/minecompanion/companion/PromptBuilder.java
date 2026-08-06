package com.minecompanion.companion;

import com.minecompanion.aiml.PredictionResult;
import com.minecompanion.chat.UserRole;
import com.minecompanion.conversation.ConversationContext;
import com.minecompanion.knowledge.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Assembles the final LLM prompt from four layers:
 *
 *   1. Persona template   — sets language style and depth for the user's role.
 *   2. Intent template    — frames the response around the classified intent / risk level.
 *   3. AIML context       — injects prediction reasons and recommendations (never raw sensor values).
 *   4. Knowledge context  — injects relevant knowledge document bodies.
 *   5. Conversation summary — injects recent conversation turns for continuity.
 *
 * All template files are loaded from the classpath at call time (no startup caching)
 * so that templates can be updated without restarting the application.
 *
 * Token budget is enforced via application.yml:
 *   prompt.budget.max-knowledge-docs
 *   prompt.budget.max-history-turns
 *   prompt.budget.max-prompt-chars
 *   prompt.budget.max-doc-body-chars
 */
@Slf4j
@Service
public class PromptBuilder {

    // ── Placeholder tokens used in every template ──────────────────────────────
    private static final String PH_AIML_CONTEXT         = "{{AIML_CONTEXT}}";
    private static final String PH_KNOWLEDGE_CONTEXT    = "{{KNOWLEDGE_CONTEXT}}";
    private static final String PH_CONVERSATION_SUMMARY = "{{CONVERSATION_SUMMARY}}";
    private static final String PH_USER_QUESTION        = "{{USER_QUESTION}}";

    // ── Intent → template file mapping ────────────────────────────────────────
    private static final Map<CompanionIntent, String> INTENT_TEMPLATES = Map.of(
        CompanionIntent.EMERGENCY_GUIDANCE,         "emergency.md",
        CompanionIntent.RISK_EXPLANATION,           "risk-{risk}.md",   // resolved at runtime
        CompanionIntent.RECOMMENDATION_EXPLANATION, "risk-{risk}.md",
        CompanionIntent.SAFETY_QUESTION,            "safety-question.md",
        CompanionIntent.EQUIPMENT_HELP,             "safety-question.md",
        CompanionIntent.PPE_GUIDANCE,               "safety-question.md",
        CompanionIntent.FIRST_AID,                  "emergency.md",
        CompanionIntent.GENERAL_MINING,             "general.md",
        CompanionIntent.SMALL_TALK,                 "small-talk.md",
        CompanionIntent.UNKNOWN,                    "general.md"
    );

    // ── Role → persona file mapping ────────────────────────────────────────────
    private static final Map<UserRole, String> PERSONA_FILES = Map.of(
        UserRole.WORKER,         "worker.md",
        UserRole.SUPERVISOR,     "supervisor.md",
        UserRole.MAINTENANCE,    "maintenance.md",
        UserRole.SAFETY_OFFICER, "safety-officer.md"
    );

    // ── Budget config ──────────────────────────────────────────────────────────
    @Value("${prompt.templates-path}")
    private String templatesPath;

    @Value("${prompt.personas-path}")
    private String personasPath;

    @Value("${prompt.budget.max-knowledge-docs}")
    private int maxKnowledgeDocs;

    @Value("${prompt.budget.max-history-turns}")
    private int maxHistoryTurns;

    @Value("${prompt.budget.max-prompt-chars}")
    private int maxPromptChars;

    @Value("${prompt.budget.max-doc-body-chars}")
    private int maxDocBodyChars;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Builds the complete LLM prompt and returns a PromptContext with diagnostic metadata.
     *
     * @param role                the user's role (controls persona)
     * @param intent              the classified intent (controls template selection)
     * @param prediction          AIML prediction result; may be null for non-prediction intents
     * @param knowledgeDocs       retrieved knowledge documents; may be empty
     * @param conversationContext conversation context from ConversationMemoryService
     * @param userQuestion        the raw user message
     * @return PromptContext containing the assembled prompt and diagnostic metadata
     */
    public PromptContext build(
            UserRole role,
            CompanionIntent intent,
            PredictionResult prediction,
            List<KnowledgeDocument> knowledgeDocs,
            ConversationContext conversationContext,
            String userQuestion) {

        int docCount  = knowledgeDocs == null ? 0 : Math.min(knowledgeDocs.size(), maxKnowledgeDocs);
        int turnCount = conversationContext == null ? 0 : conversationContext.windowSize();

        log.info("[PromptBuilder] ▶ Building prompt | role={} intent={} docs={} turns={}",
                role, intent, docCount, turnCount);

        String personaFile    = PERSONA_FILES.getOrDefault(role, "worker.md");
        String templateFile   = resolveTemplateFile(intent, prediction);

        String persona           = loadClasspathFile(personasPath + personaFile);
        String intentTemplate    = loadClasspathFile(templatesPath + templateFile);
        String aimlContext       = buildAimlContext(prediction);
        String knowledgeContext  = buildKnowledgeContext(knowledgeDocs);
        String conversationBlock = conversationContext != null ? conversationContext.toPromptBlock() : "";

        String filledTemplate = intentTemplate
                .replace(PH_AIML_CONTEXT,         aimlContext)
                .replace(PH_KNOWLEDGE_CONTEXT,    knowledgeContext)
                .replace(PH_CONVERSATION_SUMMARY, conversationBlock)
                .replace(PH_USER_QUESTION,        userQuestion);

        String raw       = persona + "\n\n" + filledTemplate;
        boolean truncated = raw.length() > maxPromptChars;
        String  prompt   = truncated ? enforceBudget(raw) : raw;

        log.info("[PromptBuilder] ◀ Prompt assembled | chars={} truncated={} template={} persona={}",
                prompt.length(), truncated, templateFile, personaFile);

        return new PromptContext(
                prompt,
                templateFile,
                personaFile,
                intent.getLabel(),
                docCount,
                turnCount,
                prompt.length(),
                truncated
        );
    }

    // ── Template loading ───────────────────────────────────────────────────────

    private String resolveTemplateFile(CompanionIntent intent, PredictionResult prediction) {
        String templateFile = INTENT_TEMPLATES.getOrDefault(intent, "general.md");
        if (templateFile.contains("{risk}")) {
            String riskLevel = (prediction != null && prediction.getPredictedRisk() != null)
                    ? prediction.getPredictedRisk().toLowerCase()
                    : "low";
            templateFile = templateFile.replace("{risk}", riskLevel);
        }
        return templateFile;
    }

    private String loadClasspathFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[PromptBuilder] Template not found: {} — using empty string", path);
            return "";
        }
    }

    // ── Context builders ───────────────────────────────────────────────────────

    private String buildAimlContext(PredictionResult prediction) {
        String risk = (prediction != null && prediction.getPredictedRisk() != null)
                ? prediction.getPredictedRisk().toUpperCase()
                : "LOW";

        StringBuilder sb = new StringBuilder();
        sb.append("--- PREDICTION & RISK ASSESSMENT (PRIMARY SOURCE OF TRUTH) ---\n");
        sb.append("Prediction Risk Level: ").append(risk).append("\n\n");

        sb.append("--- MANDATORY TONE & RESPONSE GUIDELINES (STRICT ADHERENCE REQUIRED) ---\n");
        switch (risk) {
            case "LOW":
            case "SAFE":
                sb.append("Tone: Calm, Informative, Reassuring.\n")
                  .append("Opening Instruction: Start your response by stating that the mine is currently operating under normal conditions.\n")
                  .append("Rule: State that current readings are within safe operating limits. Instruct continuing routine monitoring and standard safety procedures.\n")
                  .append("FORBIDDEN WORDS: NEVER use 'Danger', 'Unsafe', 'Evacuate', or 'Critical'. Do NOT state or imply that conditions are dangerous.\n");
                break;
            case "MEDIUM":
            case "WARNING":
                sb.append("Tone: Cautious & Attentive.\n")
                  .append("Opening Instruction: Start your response by stating that current conditions require attention.\n")
                  .append("Rule: State that sensor levels (such as methane/temperature/CO) are beginning to rise. Instruct continuing to work carefully while increasing monitoring frequency and checking ventilation.\n")
                  .append("FORBIDDEN WORDS: Do NOT talk about evacuation. Do NOT use the word 'Danger'.\n");
                break;
            case "HIGH":
                sb.append("Tone: Urgent & Strong Warning.\n")
                  .append("Opening Instruction: Start your response by stating that high-risk conditions have been detected.\n")
                  .append("Rule: State elevated gas/environmental parameters. Instruct reducing operations, improving ventilation, avoiding ignition sources, and notifying supervisors.\n");
                break;
            case "CRITICAL":
                sb.append("Tone: Immediate Emergency Evacuation.\n")
                  .append("Opening Instruction: Start your response with 'CRITICAL CONDITION DETECTED.'\n")
                  .append("Rule: Instruct immediate evacuation, stopping ignition sources, leaving the affected area, and following emergency escape procedures.\n");
                break;
            default:
                sb.append("Tone: Calm, Informative, Reassuring.\n")
                  .append("Opening Instruction: State that the mine is operating under normal conditions.\n");
                break;
        }

        if (prediction != null) {
            if (prediction.getReasons() != null && !prediction.getReasons().isEmpty()) {
                sb.append("\nSensor Explanations (Use sensor values ONLY to explain why this prediction was made):\n");
                prediction.getReasons().forEach(r -> sb.append("- ").append(r).append("\n"));
            }

            if (prediction.getRecommendations() != null && !prediction.getRecommendations().isEmpty()) {
                sb.append("\nRecommended Actions:\n");
                prediction.getRecommendations().forEach(r -> sb.append("- ").append(r).append("\n"));
            }
        }

        return sb.toString();
    }

    private String buildKnowledgeContext(List<KnowledgeDocument> docs) {
        if (docs == null || docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("--- Knowledge Base ---\n");
        int count = Math.min(docs.size(), maxKnowledgeDocs);

        for (int i = 0; i < count; i++) {
            KnowledgeDocument doc = docs.get(i);
            sb.append("## ").append(doc.getTitle()).append("\n");
            String body = doc.getBody();
            if (body != null && body.length() > maxDocBodyChars) {
                body = body.substring(0, maxDocBodyChars) + "...";
            }
            sb.append(body).append("\n\n");
        }

        return sb.toString();
    }

    // ── Budget enforcement ─────────────────────────────────────────────────────

    private String enforceBudget(String prompt) {
        if (prompt.length() <= maxPromptChars) return prompt;
        log.warn("[PromptBuilder] Prompt exceeded budget ({} chars) — truncating to {}",
                prompt.length(), maxPromptChars);
        return prompt.substring(0, maxPromptChars);
    }
}
