package com.minecompanion.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Enforces HIGH-risk prediction priority while keeping the chat fully conversational.
 *
 * When an active HIGH risk condition exists (e.g. Methane 2.5%), this policy
 * evaluates as PASS_WITH_WARNING rather than BLOCK.
 *
 * This allows the assistant to generate contextual answers to follow-up questions
 * (such as "Why is methane high?", "What should I do?", "Is oxygen level safe?")
 * while maintaining a warning status (blocked = false).
 */
@Slf4j
@Component
public class HighRiskPriorityPolicy implements SafetyPolicy {

    @Override
    public int priority() { return 50; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        // Only applies pre-LLM (llmResponse not yet set)
        if (context.getLlmResponse() != null) return PolicyVerdict.pass(name());

        if (context.getPrediction() == null) return PolicyVerdict.pass(name());

        boolean isHighRisk = "HIGH".equalsIgnoreCase(context.getPrediction().getPredictedRisk());
        if (!isHighRisk) return PolicyVerdict.pass(name());

        log.info("[HighRiskPriorityPolicy] PASS_WITH_WARNING | Active HIGH risk detected — allowing LLM conversation to continue.");

        return PolicyVerdict.passWithWarning(
                name(),
                "HIGH risk active; allowing LLM conversation to continue with live database context."
        );
    }
}
