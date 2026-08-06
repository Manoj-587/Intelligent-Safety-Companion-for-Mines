package com.minecompanion.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures emergency and first-aid responses are grounded in knowledge base content.
 *
 * If the intent requires knowledge (EMERGENCY_GUIDANCE, FIRST_AID) but no
 * documents were retrieved, the LLM must not be called — ungrounded emergency
 * advice could be incorrect and life-threatening.
 *
 * This policy ESCALATES rather than BLOCKS because the condition (no knowledge
 * available for an emergency) is a system configuration issue that requires
 * human attention, not just a user-facing block.
 *
 * Intents requiring knowledge are configured at
 * safety-policy.knowledge-required-intents.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "safety-policy")
public class EmergencyGroundingPolicy implements SafetyPolicy {

    private List<String> knowledgeRequiredIntents = List.of();

    public void setKnowledgeRequiredIntents(List<String> knowledgeRequiredIntents) {
        this.knowledgeRequiredIntents = knowledgeRequiredIntents;
    }

    @Override
    public int priority() { return 60; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        // Only applies pre-LLM
        if (context.getLlmResponse() != null) return PolicyVerdict.pass(name());

        String intentName = context.getIntent().name();
        if (!knowledgeRequiredIntents.contains(intentName)) {
            return PolicyVerdict.pass(name());
        }

        if (!context.getKnowledgeDocs().isEmpty()) {
            return PolicyVerdict.pass(name());
        }

        log.error("[EmergencyGroundingPolicy] ESCALATE — intent={} requires knowledge but none retrieved. " +
                  "Check knowledge base configuration.", intentName);

        return PolicyVerdict.escalate(
                name(),
                "Intent " + intentName + " requires knowledge documents but none were retrieved.",
                "⚠ EMERGENCY GUIDANCE\n\n" +
                "Follow these immediate steps:\n" +
                "1. Raise the alarm and contact the surface control room immediately.\n" +
                "2. Evacuate all personnel from the affected area.\n" +
                "3. Activate your self-rescuer if gas or smoke is present.\n" +
                "4. Proceed to the nearest refuge chamber or surface assembly point.\n" +
                "5. Account for all personnel and do not re-enter until declared safe.\n\n" +
                "Contact your mine safety officer for specific guidance."
        );
    }
}
