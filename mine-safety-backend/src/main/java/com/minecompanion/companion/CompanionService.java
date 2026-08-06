package com.minecompanion.companion;

import com.minecompanion.aiml.AimlApiClient;
import com.minecompanion.aiml.PredictionResult;
import com.minecompanion.chat.ChatRequest;
import com.minecompanion.chat.ChatResponse;
import com.minecompanion.chat.CompanionServicePort;
import com.minecompanion.conversation.ConversationContext;
import com.minecompanion.conversation.ConversationMemoryService;
import com.minecompanion.exception.AimlUnavailableException;
import com.minecompanion.exception.LlmException;
import com.minecompanion.knowledge.KnowledgeBaseService;
import com.minecompanion.knowledge.KnowledgeDocument;
import com.minecompanion.knowledge.KnowledgeQuery;
import com.minecompanion.llm.LlmGateway;
import com.minecompanion.llm.LlmRequest;
import com.minecompanion.llm.LlmResponse;
import com.minecompanion.safety.PolicyContext;
import com.minecompanion.safety.PolicyVerdict;
import com.minecompanion.safety.SafetyPolicyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full AI Safety Companion pipeline.
 *
 * Processing order (each stage is a private method):
 *   1.  Intent Classification
 *   2.  Conversation Context Retrieval
 *   3.  Knowledge Retrieval
 *   4.  AIML API Call          (only for prediction-relevant intents)
 *   5.  Pre-LLM Safety Policy
 *   6.  Prompt Builder
 *   7.  LLM Gateway
 *   8.  Post-LLM Safety Policy
 *   9.  Conversation Memory Update
 *   10. ChatResponse Generation
 *
 * LLM degradation:
 *   If the LLM is unavailable (LlmException), the service returns the AIML
 *   prediction, reasons, and recommendations directly with a degraded=true flag.
 *   No exception is propagated to the caller.
 *
 * Provider independence:
 *   This class depends only on LlmGateway — it never references a provider name.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionService implements CompanionServicePort {

    private final IntentClassifier        intentClassifier;
    private final ConversationMemoryService conversationMemory;
    private final KnowledgeBaseService    knowledgeBase;
    private final AimlApiClient           aimlApiClient;
    private final SafetyPolicyEngine      policyEngine;
    private final PromptBuilder           promptBuilder;
    private final LlmGateway             llmGateway;
    private final com.minecompanion.persistence.service.SensorReadingService sensorReadingService;
    private final com.minecompanion.persistence.service.PredictionHistoryService predictionHistoryService;

    @Value("${knowledge.max-sections-per-query}")
    private int maxKnowledgeDocs;

    // ── Intents that require an AIML prediction ────────────────────────────────
    private static final java.util.Set<CompanionIntent> PREDICTION_INTENTS = java.util.Set.of(
            CompanionIntent.RISK_EXPLANATION,
            CompanionIntent.RECOMMENDATION_EXPLANATION
    );

    // ── Pipeline entry point ───────────────────────────────────────────────────

    @Override
    public ChatResponse process(ChatRequest request) {
        long pipelineStart = now();
        String sessionId   = request.getSessionId();
        String message     = request.getMessage();

        log.info("[CompanionService] ═══ Pipeline START | session={} role={}", sessionId, request.getRole());

        // ── Stage 1: Intent Classification ────────────────────────────────────
        long s1 = now();
        String lastAiReply = conversationMemory.getLastAiReply(sessionId);
        IntentClassifier.ClassificationResult classification =
                intentClassifier.classify(message, lastAiReply);
        CompanionIntent intent     = classification.intent();
        double          confidence = classification.confidence();
        List<String>    keywords   = classification.keywords();
        logStage(1, "Intent Classification", s1,
                "intent=" + intent + " confidence=" + confidence);

        // ── Stage 2: Conversation Context Retrieval ────────────────────────────
        long s2 = now();
        ConversationContext convContext = conversationMemory.getContext(sessionId);
        logStage(2, "Conversation Context", s2,
                "turns=" + convContext.windowSize() + " topic=" + convContext.currentTopic());

        // ── Stage 3: Knowledge Retrieval ───────────────────────────────────────
        long s3 = now();
        List<KnowledgeDocument> knowledgeDocs = retrieveKnowledge(intent, keywords, request);
        logStage(3, "Knowledge Retrieval", s3, "docs=" + knowledgeDocs.size());

        // ── Stage 4: AIML API Call ─────────────────────────────────────────────
        long s4 = now();
        PredictionResult prediction = fetchPrediction(intent, sessionId);
        logStage(4, "AIML API", s4,
                prediction != null ? "risk=" + prediction.getPredictedRisk() : "skipped");

        // ── Stage 5: Pre-LLM Safety Policy ────────────────────────────────────
        long s5 = now();
        PolicyContext policyCtx = PolicyContext.builder()
                .userMessage(message)
                .intent(intent)
                .confidence(confidence)
                .role(request.getRole())
                .prediction(prediction)
                .knowledgeDocs(knowledgeDocs)
                .build();

        PolicyVerdict preLlmVerdict = policyEngine.evaluatePreLlm(policyCtx);
        logStage(5, "Pre-LLM Safety Policy", s5, "verdict=" + preLlmVerdict.getVerdictType());

        if (!preLlmVerdict.isPassed()) {
            log.info("[CompanionService] ═══ Pipeline BLOCKED | policy={} totalMs={}",
                    preLlmVerdict.getPolicyName(), elapsed(pipelineStart));
            return blockedResponse(preLlmVerdict, intent, knowledgeDocs, prediction, confidence);
        }

        // ── Stage 6: Prompt Builder ────────────────────────────────────────────
        long s6 = now();
        PromptContext promptCtx = promptBuilder.build(
                request.getRole(), intent, prediction, knowledgeDocs, convContext, message);
        logStage(6, "Prompt Builder", s6,
                "template=" + promptCtx.templateUsed()
                + " persona=" + promptCtx.personaUsed()
                + " chars=" + promptCtx.totalChars()
                + " truncated=" + promptCtx.truncated());

        // ── Stage 7: LLM Gateway ───────────────────────────────────────────────
        long s7 = now();
        LlmResponse llmResponse;
        try {
            llmResponse = llmGateway.call(new LlmRequest(promptCtx.prompt()));
            logStage(7, "LLM Gateway", s7,
                    "provider=" + llmResponse.provider()
                    + " model=" + llmResponse.model()
                    + " latencyMs=" + llmResponse.latencyMs()
                    + " replyChars=" + llmResponse.replyChars());
        } catch (LlmException ex) {
            logStage(7, "LLM Gateway", s7, "DEGRADED — " + ex.getMessage());
            log.warn("[CompanionService] LLM unavailable — returning AIML degraded response | session={}", sessionId);
            ChatResponse degraded = degradedResponse(prediction, intent, knowledgeDocs, promptCtx);
            updateMemory(sessionId, message, degraded.getReply(), intent);
            log.info("[CompanionService] ═══ Pipeline DEGRADED | totalMs={}", elapsed(pipelineStart));
            return degraded;
        }

        // ── Stage 8: Post-LLM Safety Policy ───────────────────────────────────
        long s8 = now();
        PolicyContext postCtx = PolicyContext.builder()
                .userMessage(message)
                .intent(intent)
                .confidence(confidence)
                .role(request.getRole())
                .prediction(prediction)
                .knowledgeDocs(knowledgeDocs)
                .llmResponse(llmResponse.reply())
                .build();

        PolicyVerdict postLlmVerdict = policyEngine.evaluatePostLlm(postCtx);
        logStage(8, "Post-LLM Safety Policy", s8, "verdict=" + postLlmVerdict.getVerdictType());

        String finalReply;
        boolean wasBlocked;
        if (!postLlmVerdict.isPassed()) {
            log.warn("[CompanionService] Post-LLM policy blocked LLM reply | policy={}",
                    postLlmVerdict.getPolicyName());
            finalReply = postLlmVerdict.getPreApprovedReply();
            wasBlocked = true;
        } else {
            finalReply = llmResponse.reply();
            wasBlocked = false;
        }

        // ── Stage 9: Conversation Memory Update ───────────────────────────────
        long s9 = now();
        updateMemory(sessionId, message, finalReply, intent);
        logStage(9, "Conversation Memory Update", s9, "recorded");

        // ── Stage 10: ChatResponse Generation ─────────────────────────────────
        long s10 = now();
        ChatResponse response = buildResponse(finalReply, intent, knowledgeDocs, prediction,
                promptCtx, wasBlocked, false);
        logStage(10, "Response Generation", s10, "sources=" + response.getSources().size());

        log.info("[CompanionService] ═══ Pipeline COMPLETE | session={} intent={} totalMs={}",
                sessionId, intent, elapsed(pipelineStart));

        return response;
    }

    // ── Stage implementations ──────────────────────────────────────────────────

    private List<KnowledgeDocument> retrieveKnowledge(
            CompanionIntent intent, List<String> keywords, ChatRequest request) {

        KnowledgeQuery query = KnowledgeQuery.builder()
                .intent(intent.name())
                .keywords(keywords)
                .userRole(request.getRole().name().toLowerCase())
                .categoryHint(intent.getCategoryHint())
                .maxResults(maxKnowledgeDocs)
                .build();

        return knowledgeBase.retrieve(query);
    }

    private PredictionResult fetchPrediction(CompanionIntent intent, String sessionId) {
        java.util.Optional<com.minecompanion.persistence.entity.SensorReading> latestReadingOpt =
                sensorReadingService.getLatest();
        java.util.Optional<com.minecompanion.persistence.entity.PredictionHistory> latestPredictionOpt =
                predictionHistoryService.getLatest();

        if (latestReadingOpt.isPresent()) {
            com.minecompanion.persistence.entity.SensorReading reading = latestReadingOpt.get();
            String riskLevel = "MEDIUM";
            List<String> recommendations = new ArrayList<>();

            if (latestPredictionOpt.isPresent()) {
                com.minecompanion.persistence.entity.PredictionHistory history = latestPredictionOpt.get();
                riskLevel = history.getRiskLevel();
                if (history.getRecommendation() != null && !history.getRecommendation().isBlank()) {
                    recommendations.add(history.getRecommendation());
                }
            } else {
                try {
                    Map<String, Object> payload = buildFlaskPayload(reading);
                    PredictionResult res = aimlApiClient.predict(payload);
                    riskLevel = res.getPredictedRisk();
                    recommendations.addAll(res.getRecommendations());
                } catch (Exception ex) {
                    log.warn("[CompanionService] AIML prediction fallback when loading live sensor reading: {}", ex.getMessage());
                }
            }

            List<String> reasons = new ArrayList<>();
            if (reading.getMethane() != null) reasons.add("Methane: " + reading.getMethane() + "%");
            if (reading.getTemperature() != null) reasons.add("Temperature: " + reading.getTemperature() + "°C");
            if (reading.getHumidity() != null) reasons.add("Humidity: " + reading.getHumidity() + "%");
            if (reading.getCarbonMonoxide() != null) reasons.add("Carbon Monoxide: " + reading.getCarbonMonoxide() + " ppm");
            if (reading.getOxygen() != null) reasons.add("Oxygen: " + reading.getOxygen() + "%");
            if (reading.getAirflow() != null) reasons.add("Airflow: " + reading.getAirflow() + " m³/s");
            if (reading.getPressure() != null) reasons.add("Pressure: " + reading.getPressure() + " kPa");

            return PredictionResult.builder()
                    .predictedRisk(riskLevel)
                    .reasons(reasons)
                    .recommendations(recommendations)
                    .build();
        }

        if (PREDICTION_INTENTS.contains(intent)) {
            try {
                return aimlApiClient.predict(Map.of());
            } catch (AimlUnavailableException ex) {
                log.warn("[CompanionService] AIML unavailable for intent={} — proceeding without prediction: {}",
                        intent, ex.getMessage());
                return null;
            }
        }

        return null;
    }

    private Map<String, Object> buildFlaskPayload(com.minecompanion.persistence.entity.SensorReading r) {
        Map<String, Object> map = new java.util.HashMap<>();
        // Environmental & gas readings
        map.put("AN311", r.getCarbonMonoxide() != null ? r.getCarbonMonoxide() : 10.0);
        map.put("AN422", r.getMethane() != null ? r.getMethane() : 0.5);
        map.put("AN423", 0.1);
        map.put("TP1721", r.getTemperature() != null ? r.getTemperature() : 25.0);
        map.put("RH1722", r.getHumidity() != null ? r.getHumidity() : 50.0);
        map.put("BA1723", r.getPressure() != null ? r.getPressure() : 101.3);
        map.put("TP1711", 24.5);
        map.put("RH1712", 48.0);
        map.put("BA1713", r.getAirflow() != null ? r.getAirflow() : 2.5);
        // Mine monitoring channels
        map.put("MM252", 0.0);
        map.put("MM261", 0.0);
        map.put("MM262", 0.0);
        map.put("MM263", 0.0);
        map.put("MM264", r.getOxygen() != null ? r.getOxygen() : 20.9);
        map.put("MM256", 0.0);
        map.put("MM211", 0.0);
        map.put("CM861", 0.0);
        map.put("CR863", 0.0);
        map.put("P_864", 0.0);
        map.put("TC862", 0.0);
        map.put("WM868", 0.0);
        // Infrared & current sensors
        map.put("AMP1_IR", 12.0);
        map.put("AMP2_IR", 12.5);
        map.put("DMP3_IR", 5.0);
        map.put("DMP4_IR", 5.2);
        map.put("AMP5_IR", 11.8);
        map.put("F_SIDE", 1.0);
        return map;
    }

    private void updateMemory(String sessionId, String userMessage,
                              String aiReply, CompanionIntent intent) {
        try {
            conversationMemory.recordTurn(sessionId, userMessage, aiReply, intent);
        } catch (Exception ex) {
            // Memory update failure must never break the response pipeline
            log.warn("[CompanionService] Failed to update conversation memory | session={} error={}",
                    sessionId, ex.getMessage());
        }
    }

    // ── Response builders ──────────────────────────────────────────────────────

    private ChatResponse blockedResponse(PolicyVerdict verdict, CompanionIntent intent,
                                         List<KnowledgeDocument> docs, PredictionResult prediction,
                                         double confidence) {
        ChatResponse response = buildResponse(
                verdict.getPreApprovedReply(), intent, docs, prediction, null, true, false);

        ChatResponse.Diagnostics diag = ChatResponse.Diagnostics.builder()
                .blockedBy(verdict.getPolicyName())
                .blockReason(verdict.getReason())
                .confidence(confidence)
                .build();
        response.setDiagnostics(diag);

        return response;
    }

    private ChatResponse degradedResponse(PredictionResult prediction, CompanionIntent intent,
                                          List<KnowledgeDocument> docs, PromptContext promptCtx) {
        String reply = buildDegradedReply(prediction);
        return buildResponse(reply, intent, docs, prediction, promptCtx, false, true);
    }

    /**
     * Builds a human-readable fallback reply from the AIML prediction alone.
     * Called when the LLM is unavailable. Never fabricates sensor values.
     */
    private String buildDegradedReply(PredictionResult prediction) {
        if (prediction == null) {
            return "⚠ The AI assistant is temporarily unavailable. " +
                   "Please contact your mine safety officer for guidance.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠ AI assistant is temporarily unavailable. ")
          .append("Here is the current safety status from the monitoring system:\n\n");

        sb.append("**Risk Level: ").append(prediction.getPredictedRisk()).append("**\n\n");

        if (!prediction.getReasons().isEmpty()) {
            sb.append("**Reasons:**\n");
            prediction.getReasons().forEach(r -> sb.append("- ").append(r).append("\n"));
            sb.append("\n");
        }

        if (!prediction.getRecommendations().isEmpty()) {
            sb.append("**Recommended Actions:**\n");
            prediction.getRecommendations().forEach(r -> sb.append("- ").append(r).append("\n"));
            sb.append("\n");
        }

        sb.append("Please follow these recommendations and contact your safety officer if unsure.");
        return sb.toString();
    }

    private ChatResponse buildResponse(String reply, CompanionIntent intent,
                                       List<KnowledgeDocument> docs, PredictionResult prediction,
                                       PromptContext promptCtx, boolean blocked, boolean degraded) {
        List<String> sources = buildSources(docs, prediction);

        ChatResponse.Diagnostics diagnostics = promptCtx == null ? null :
                ChatResponse.Diagnostics.builder()
                        .templateUsed(promptCtx.templateUsed())
                        .personaUsed(promptCtx.personaUsed())
                        .knowledgeDocCount(promptCtx.knowledgeDocCount())
                        .conversationTurnCount(promptCtx.conversationTurnCount())
                        .totalPromptChars(promptCtx.totalChars())
                        .promptTruncated(promptCtx.truncated())
                        .build();

        String riskLevel = prediction != null && prediction.getPredictedRisk() != null
                ? prediction.getPredictedRisk()
                : "SAFE";

        return ChatResponse.builder()
                .reply(reply)
                .intent(intent.getLabel())
                .sources(sources)
                .blocked(blocked)
                .degraded(degraded)
                .riskLevel(riskLevel)
                .diagnostics(diagnostics)
                .build();
    }

    private List<String> buildSources(List<KnowledgeDocument> docs, PredictionResult prediction) {
        List<String> sources = new ArrayList<>();
        if (prediction != null) {
            sources.add("prediction_engine");
        }
        docs.stream()
                .map(d -> "knowledge:" + d.getId())
                .forEach(sources::add);
        return sources;
    }

    // ── Logging helpers ────────────────────────────────────────────────────────

    private void logStage(int stage, String name, long stageStart, String detail) {
        log.info("[CompanionService] Stage {} | {} | {}ms | {}",
                stage, name, elapsed(stageStart), detail);
    }

    private long now()              { return Instant.now().toEpochMilli(); }
    private long elapsed(long from) { return now() - from; }
}
