package com.minecompanion.companion;

/**
 * Result of PromptBuilder.build().
 *
 * Carries the assembled prompt text alongside diagnostic metadata so
 * CompanionService can log and surface build details without re-computing them.
 *
 * @param prompt            the fully assembled LLM prompt string
 * @param templateUsed      filename of the intent template selected (e.g. "risk-high.md")
 * @param personaUsed       filename of the persona template selected (e.g. "worker.md")
 * @param intent            string label of the classified intent
 * @param knowledgeDocCount number of knowledge documents injected
 * @param conversationTurnCount number of conversation turns injected
 * @param totalChars        total character length of the assembled prompt
 * @param truncated         true if the prompt was cut to fit the budget
 */
public record PromptContext(
        String  prompt,
        String  templateUsed,
        String  personaUsed,
        String  intent,
        int     knowledgeDocCount,
        int     conversationTurnCount,
        int     totalChars,
        boolean truncated
) {}
