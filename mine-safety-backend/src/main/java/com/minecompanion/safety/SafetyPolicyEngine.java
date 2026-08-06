package com.minecompanion.safety;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates all SafetyPolicy implementations.
 *
 * Runs pre-LLM and post-LLM safety evaluation chains.
 * Logs every policy evaluation with Policy Name, Result, Reason, Confidence, and Time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyPolicyEngine {

    private final List<SafetyPolicy> policies;

    private List<SafetyPolicy> preLlmPolicies;
    private List<SafetyPolicy> postLlmPolicies;

    @PostConstruct
    public void init() {
        List<SafetyPolicy> sorted = policies.stream()
                .sorted(Comparator.comparingInt(SafetyPolicy::priority))
                .collect(Collectors.toList());

        preLlmPolicies = sorted.stream()
                .filter(p -> !(p instanceof PredictionContradictionPolicy))
                .collect(Collectors.toList());

        postLlmPolicies = sorted.stream()
                .filter(p -> p instanceof PredictionContradictionPolicy)
                .collect(Collectors.toList());

        log.info("[SafetyPolicyEngine] Initialized with {} policy/policies total | pre-LLM={} post-LLM={}",
                sorted.size(), preLlmPolicies.size(), postLlmPolicies.size());

        sorted.forEach(p -> log.info("[SafetyPolicyEngine]   priority={} policy={}",
                p.priority(), p.name()));
    }

    public PolicyVerdict evaluatePreLlm(PolicyContext context) {
        return runChain(preLlmPolicies, context, "pre-LLM");
    }

    public PolicyVerdict evaluatePostLlm(PolicyContext context) {
        return runChain(postLlmPolicies, context, "post-LLM");
    }

    private PolicyVerdict runChain(List<SafetyPolicy> chain, PolicyContext context, String phase) {
        log.debug("[SafetyPolicyEngine] Running {} policy chain ({} policies)", phase, chain.size());

        for (SafetyPolicy policy : chain) {
            long startTime = System.nanoTime();
            PolicyVerdict verdict = policy.evaluate(context);
            long executionTimeMs = Math.round((System.nanoTime() - startTime) / 1_000_000.0);

            log.info("[Safety]\nPolicy={}\nResult={}\nReason={}\nConfidence={}\nTime={}ms",
                    policy.name(),
                    verdict.getVerdictType(),
                    verdict.getReason() != null ? verdict.getReason() : "None",
                    context.getConfidence(),
                    executionTimeMs);

            if (verdict.isEscalated()) {
                log.error("[SafetyPolicyEngine] ESCALATE | phase={} policy={} reason={}",
                        phase, verdict.getPolicyName(), verdict.getReason());
                return verdict;
            }

            if (verdict.isBlocked()) {
                log.warn("[SafetyPolicyEngine] BLOCK | phase={} policy={} reason={}",
                        phase, verdict.getPolicyName(), verdict.getReason());
                return verdict;
            }
        }

        return PolicyVerdict.pass("SafetyPolicyEngine");
    }
}
