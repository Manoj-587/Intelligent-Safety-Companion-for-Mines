package com.minecompanion.llm;

/**
 * Provider-independent input to LlmGateway.
 *
 * Contains only the fully assembled prompt string produced by PromptBuilder.
 * No provider-specific fields belong here — each gateway implementation
 * wraps this into its own request body format internally.
 *
 * @param prompt the complete prompt text to send to the LLM
 */
public record LlmRequest(String prompt) {

    public int promptChars() {
        return prompt == null ? 0 : prompt.length();
    }
}
