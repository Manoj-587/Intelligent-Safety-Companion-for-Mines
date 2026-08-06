package com.minecompanion.safety;

import com.minecompanion.aiml.PredictionResult;
import com.minecompanion.chat.UserRole;
import com.minecompanion.companion.CompanionIntent;
import com.minecompanion.knowledge.KnowledgeDocument;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Immutable input record for the SafetyPolicyEngine.
 *
 * Built once by CompanionService after intent classification and knowledge
 * retrieval, then passed unchanged through every SafetyPolicy evaluation.
 *
 * Pre-LLM fields (always populated):
 *   userMessage, intent, confidence, role, prediction, knowledgeDocs
 *
 * Post-LLM field (populated only during the post-generation check):
 *   llmResponse — null during pre-LLM evaluation; set after the LLM replies.
 *
 * Keeping pre- and post-LLM context in one record means policies can be
 * reused for both evaluation phases without changing their signatures.
 */
@Data
@Builder
public class PolicyContext {

    /** The raw user message exactly as received. */
    private String userMessage;

    /** The classified intent from IntentClassifier. */
    private CompanionIntent intent;

    /** Classification confidence score (0.0 – 1.0). */
    private double confidence;

    /** The role of the user making the request. */
    private UserRole role;

    /**
     * The latest AIML prediction result for this session.
     * Null when no prediction has been made yet (e.g. first message).
     */
    private PredictionResult prediction;

    /**
     * Knowledge documents retrieved for this request.
     * Empty list when no documents matched the query.
     */
    @Builder.Default
    private List<KnowledgeDocument> knowledgeDocs = List.of();

    /**
     * The LLM-generated response text.
     * Null during pre-LLM policy evaluation.
     * Set by CompanionService before the post-generation policy check.
     */
    private String llmResponse;
}
