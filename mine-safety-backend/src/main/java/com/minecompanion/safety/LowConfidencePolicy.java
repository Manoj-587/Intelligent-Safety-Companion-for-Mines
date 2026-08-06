package com.minecompanion.safety;

import com.minecompanion.companion.CompanionIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rejects requests where intent classification confidence is below the
 * configured minimum threshold.
 *
 * A low-confidence classification means the user's message is ambiguous.
 * Sending an ambiguous message to the LLM risks an irrelevant or unsafe
 * response. It is safer to ask the user to clarify.
 *
 * SMALL_TALK and UNKNOWN are exempt — they are handled directly by
 * CompanionService without an LLM call, so confidence is irrelevant.
 *
 * Threshold is configured at safety-policy.min-confidence-threshold.
 */
@Slf4j
@Component
public class LowConfidencePolicy implements SafetyPolicy {

    @Value("${safety-policy.min-confidence-threshold}")
    private double minConfidenceThreshold;

    @Override
    public int priority() { return 30; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        CompanionIntent intent = context.getIntent();

        // SMALL_TALK and UNKNOWN are handled without LLM — skip confidence check
        if (intent == CompanionIntent.SMALL_TALK || intent == CompanionIntent.UNKNOWN) {
            return PolicyVerdict.pass(name());
        }

        if (context.getConfidence() < minConfidenceThreshold) {
            log.warn("[LowConfidencePolicy] Confidence below threshold | intent={} confidence={} threshold={}",
                    intent, context.getConfidence(), minConfidenceThreshold);
            return PolicyVerdict.block(
                    name(),
                    "Classification confidence " + context.getConfidence() + " below threshold " + minConfidenceThreshold,
                    "I'm not sure I understood your question. Could you please rephrase it? " +
                    "For example: 'What is methane?', 'Why is the risk HIGH?', or 'What should I do in a fire?'"
            );
        }

        return PolicyVerdict.pass(name());
    }
}
