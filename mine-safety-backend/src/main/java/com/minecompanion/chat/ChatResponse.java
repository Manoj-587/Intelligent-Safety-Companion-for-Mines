package com.minecompanion.chat;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Outbound DTO for POST /api/chat.
 *
 * reply        — the AI-generated response text (Markdown).
 * intent       — the classified intent; React can use this to render intent badges.
 * sources      — knowledge sources used to ground the response.
 * blocked      — true when SafetyGuard intercepted the request.
 * degraded     — true when the LLM was unavailable and the AIML fallback was used.
 * diagnostics  — prompt build metadata; null when blocked before prompt assembly.
 */
@Data
@Builder
public class ChatResponse {

    private String       reply;
    private String       intent;
    private List<String> sources;
    private boolean      blocked;
    private boolean      degraded;
    private String       riskLevel;
    private Diagnostics  diagnostics;

    /**
     * Prompt build metadata surfaced for transparency and debugging.
     * Populated from PromptContext after every successful prompt assembly.
     */
    @Data
    @Builder
    public static class Diagnostics {
        private String  templateUsed;
        private String  personaUsed;
        private int     knowledgeDocCount;
        private int     conversationTurnCount;
        private int     totalPromptChars;
        private boolean promptTruncated;
        private String  blockedBy;
        private String  blockReason;
        private Double  confidence;
    }
}
