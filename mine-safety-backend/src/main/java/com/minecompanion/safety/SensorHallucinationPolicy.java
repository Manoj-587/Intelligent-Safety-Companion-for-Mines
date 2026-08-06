package com.minecompanion.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prevents sensor value hallucination.
 *
 * When the user asks to explain a prediction or recommendation, the LLM
 * must ground its response in actual AIML output. If no prediction data
 * is available (Flask unreachable, session has no prediction yet), the
 * LLM must not be called — it would invent sensor values.
 *
 * Intents that require prediction data are configured at
 * safety-policy.prediction-required-intents.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "safety-policy")
public class SensorHallucinationPolicy implements SafetyPolicy {

    private List<String> predictionRequiredIntents = List.of();

    public void setPredictionRequiredIntents(List<String> predictionRequiredIntents) {
        this.predictionRequiredIntents = predictionRequiredIntents;
    }

    @Override
    public int priority() { return 40; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        String intentName = context.getIntent().name();

        if (!predictionRequiredIntents.contains(intentName)) {
            return PolicyVerdict.pass(name());
        }

        if (context.getPrediction() == null) {
            log.warn("[SensorHallucinationPolicy] Prediction data absent for intent={} — blocking to prevent hallucination",
                    intentName);
            return PolicyVerdict.block(
                    name(),
                    "Prediction data required for intent " + intentName + " but is absent.",
                    "I cannot explain the current prediction because the safety monitoring system " +
                    "has not returned any data yet. Please check the sensor dashboard directly, " +
                    "or ask your supervisor to review the latest readings."
            );
        }

        return PolicyVerdict.pass(name());
    }
}
