package com.minecompanion.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prevents LLM responses from contradicting a HIGH-risk AIML prediction.
 *
 * Runs post-LLM only. If the AIML prediction is HIGH and the LLM response
 * contains a contradiction keyword (e.g. "no risk", "safe"), the response is
 * safely rewritten to present the real database sensor data and recommendations.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "safety-policy")
public class PredictionContradictionPolicy implements SafetyPolicy {

    private List<String> contradictionKeywords = List.of();

    public void setContradictionKeywords(List<String> contradictionKeywords) {
        this.contradictionKeywords = contradictionKeywords;
    }

    @Override
    public int priority() { return 70; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        // Only applies post-LLM
        if (context.getLlmResponse() == null) return PolicyVerdict.pass(name());

        if (context.getPrediction() == null) return PolicyVerdict.pass(name());

        boolean isHighRisk = "HIGH".equalsIgnoreCase(context.getPrediction().getPredictedRisk());
        if (!isHighRisk) return PolicyVerdict.pass(name());

        String lowerResponse = context.getLlmResponse().toLowerCase();

        for (String keyword : contradictionKeywords) {
            if (lowerResponse.contains(keyword.toLowerCase())) {
                log.warn("[PredictionContradictionPolicy] LLM response contradicts HIGH risk prediction | keyword=\"{}\"",
                        keyword);

                String liveSensors = context.getPrediction().getReasons().isEmpty()
                        ? "Methane & environmental sensors outside safe operating limits."
                        : String.join(" | ", context.getPrediction().getReasons());

                String liveRecommendations = context.getPrediction().getRecommendations().isEmpty()
                        ? "Evacuate area and notify safety officer immediately."
                        : String.join("\n- ", context.getPrediction().getRecommendations());

                return PolicyVerdict.block(
                        name(),
                        "LLM response contained contradiction keyword \"" + keyword + "\" while risk is HIGH.",
                        "⚠️ HIGH RISK CONDITION DETECTED\n\n" +
                        "**Current Sensor Readings:**\n- " + liveSensors + "\n\n" +
                        "**Prediction:** HIGH Risk\n\n" +
                        "**Immediate Actions Required:**\n- " + liveRecommendations + "\n\n" +
                        "Safety protocol override applied: Follow emergency recommendations immediately."
                );
            }
        }

        return PolicyVerdict.pass(name());
    }
}
