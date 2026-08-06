package com.minecompanion.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Detects and blocks prompt injection attempts.
 *
 * Runs first (priority 10) so injected messages never reach the LLM,
 * the knowledge base, or any other policy.
 *
 * Injection patterns are configured in application.yml under
 * safety-policy.injection-patterns and can be extended without
 * touching this class.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "safety-policy")
public class PromptInjectionPolicy implements SafetyPolicy {

    private List<String> injectionPatterns = List.of();

    public void setInjectionPatterns(List<String> injectionPatterns) {
        this.injectionPatterns = injectionPatterns;
    }

    @Override
    public int priority() { return 10; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        String lower = context.getUserMessage().toLowerCase();

        for (String pattern : injectionPatterns) {
            if (lower.contains(pattern.toLowerCase())) {
                log.warn("[PromptInjectionPolicy] Injection attempt detected | pattern=\"{}\" session message truncated",
                        pattern);
                return PolicyVerdict.block(
                        name(),
                        "Prompt injection pattern detected: " + pattern,
                        "I can only assist with mine safety, equipment, and emergency topics. " +
                        "Please ask a mining-related question."
                );
            }
        }

        return PolicyVerdict.pass(name());
    }
}
