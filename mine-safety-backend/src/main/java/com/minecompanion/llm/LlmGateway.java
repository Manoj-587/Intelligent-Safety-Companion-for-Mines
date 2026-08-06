package com.minecompanion.llm;

/**
 * Port interface for the LLM gateway.
 *
 * CompanionService depends only on this interface.
 * Implemented by OpenRouterGateway using the official OpenAI Java SDK.
 */
public interface LlmGateway {

    /**
     * Sends the prompt to OpenRouter and returns an LlmResponse.
     *
     * @param request the assembled prompt wrapped in an LlmRequest
     * @return LlmResponse containing the reply text and diagnostic metadata
     * @throws com.minecompanion.exception.LlmException on any provider failure
     */
    LlmResponse call(LlmRequest request);

    /** Returns the provider name ("openrouter"). Used for logging only. */
    String providerName();

    /** Returns the configured model name. Used for logging only. */
    String modelName();
}
