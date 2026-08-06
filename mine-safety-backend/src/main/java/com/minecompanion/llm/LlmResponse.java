package com.minecompanion.llm;

/**
 * Provider-independent output from LlmGateway.
 *
 * CompanionService reads only this record — it never sees provider-specific
 * response shapes (Gemini candidates[], OpenAI choices[], Ollama message{}).
 *
 * @param reply        the generated text extracted from the provider response
 * @param provider     the provider that produced this response (e.g. "gemini")
 * @param model        the model name used (e.g. "gemini-1.5-flash")
 * @param latencyMs    wall-clock time from request send to response received
 * @param promptChars  character count of the prompt that was sent
 * @param replyChars   character count of the reply text
 */
public record LlmResponse(
        String reply,
        String provider,
        String model,
        long   latencyMs,
        int    promptChars,
        int    replyChars
) {
    public static LlmResponse of(String reply, String provider, String model,
                                 long latencyMs, int promptChars) {
        return new LlmResponse(reply, provider, model, latencyMs, promptChars,
                reply == null ? 0 : reply.length());
    }
}
