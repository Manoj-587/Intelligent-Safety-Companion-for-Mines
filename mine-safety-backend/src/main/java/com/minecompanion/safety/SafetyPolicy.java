package com.minecompanion.safety;

/**
 * Extension point for the Safety Policy Engine.
 *
 * Each safety responsibility is implemented as a separate class that
 * implements this interface and is annotated with @Component.
 * SafetyPolicyEngine discovers all implementations via Spring injection
 * and evaluates them in priority order.
 *
 * Adding a new policy:
 *   1. Create a class that implements SafetyPolicy.
 *   2. Annotate it with @Component.
 *   3. Set its priority() to control evaluation order.
 *   That is all — SafetyPolicyEngine requires no changes.
 *
 * Current implementations (in evaluation order):
 *   1. PromptInjectionPolicy        — blocks injection attempts before anything else
 *   2. RoleAuthorizationPolicy      — blocks unauthorized role/intent combinations
 *   3. LowConfidencePolicy          — blocks requests below confidence threshold
 *   4. SensorHallucinationPolicy    — blocks prediction explanations without AIML data
 *   5. HighRiskPriorityPolicy       — enforces HIGH-risk alert before unrelated answers
 *   6. EmergencyGroundingPolicy     — escalates emergencies without knowledge backing
 *   7. PredictionContradictionPolicy— blocks post-LLM responses that contradict HIGH risk
 */
public interface SafetyPolicy {

    /**
     * Evaluates this policy against the given context.
     *
     * @param context all signals available at evaluation time
     * @return PASS if the policy is satisfied; BLOCK or ESCALATE otherwise
     */
    PolicyVerdict evaluate(PolicyContext context);

    /**
     * Evaluation order — lower number runs first.
     * Policies that can short-circuit early (injection, auth) should have
     * the lowest priority numbers.
     */
    int priority();

    /** Human-readable name used in logs and audit trails. */
    default String name() {
        return this.getClass().getSimpleName();
    }
}
