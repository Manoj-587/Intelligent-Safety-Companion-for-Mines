package com.minecompanion.safety;

import lombok.Builder;
import lombok.Data;

/**
 * Immutable result of a single SafetyPolicy evaluation.
 */
@Data
@Builder
public class PolicyVerdict {

    private VerdictType verdictType;
    private String      policyName;
    private String      reason;
    private String      preApprovedReply;

    /** Policy is satisfied — processing continues. */
    public static PolicyVerdict pass(String policyName) {
        return PolicyVerdict.builder()
                .verdictType(VerdictType.PASS)
                .policyName(policyName)
                .reason("Policy satisfied.")
                .build();
    }

    /** Policy is satisfied with a safety warning — chat remains conversational (blocked = false). */
    public static PolicyVerdict passWithWarning(String policyName, String reason) {
        return PolicyVerdict.builder()
                .verdictType(VerdictType.PASS_WITH_WARNING)
                .policyName(policyName)
                .reason(reason)
                .build();
    }

    /** Policy is violated — LLM is not called; preApprovedReply is returned. */
    public static PolicyVerdict block(String policyName, String reason, String preApprovedReply) {
        return PolicyVerdict.builder()
                .verdictType(VerdictType.BLOCK)
                .policyName(policyName)
                .reason(reason)
                .preApprovedReply(preApprovedReply)
                .build();
    }

    /** Critical condition requiring human intervention — logged at ERROR level. */
    public static PolicyVerdict escalate(String policyName, String reason, String preApprovedReply) {
        return PolicyVerdict.builder()
                .verdictType(VerdictType.ESCALATE)
                .policyName(policyName)
                .reason(reason)
                .preApprovedReply(preApprovedReply)
                .build();
    }

    public boolean isPassed()    { return verdictType == VerdictType.PASS || verdictType == VerdictType.PASS_WITH_WARNING; }
    public boolean isBlocked()   { return verdictType == VerdictType.BLOCK; }
    public boolean isEscalated() { return verdictType == VerdictType.ESCALATE; }
}
