package com.minecompanion.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.minecompanion.exception.LlmException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter LlmGateway implementation using Spring WebClient.
 * Reads llm.base-url, llm.api-key, llm.model, llm.max-tokens, and llm.timeout-ms.
 * Safely parses choices[0].message.content while ignoring extra OpenRouter response fields.
 */
@Slf4j
@Component
public class OpenRouterGateway implements LlmGateway {

    private static final String PROVIDER = "openrouter";

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;

    public OpenRouterGateway(
            @Value("${llm.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.model:openai/gpt-4.1-mini}") String model,
            @Value("${llm.max-tokens:1024}") int maxTokens,
            @Value("${llm.timeout-ms:30000}") int timeoutMs) {

        this.model = model;
        this.maxTokens = maxTokens;

        String formattedApiKey = apiKey != null ? apiKey.trim() : "";
        if (formattedApiKey.startsWith("\"") && formattedApiKey.endsWith("\"")) {
            formattedApiKey = formattedApiKey.substring(1, formattedApiKey.length() - 1);
        }

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (!formattedApiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + formattedApiKey);
        }

        this.webClient = builder.build();
    }

    @Override
    public LlmResponse call(LlmRequest request) {
        log.info("[LlmGateway] ▶ Calling {} | provider={} model={} maxTokens={} promptChars={}",
                PROVIDER, PROVIDER, model, maxTokens, request.promptChars());

        long start = Instant.now().toEpochMilli();

        Map<String, Object> payload = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "user", "content", request.prompt())
                )
        );

        try {
            OpenRouterChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenRouterChatResponse.class)
                    .block();

            long latency = Instant.now().toEpochMilli() - start;
            String reply = extractReply(response);

            log.info("[LlmGateway] ◀ {} responded | provider={} model={} latencyMs={} replyChars={}",
                    PROVIDER, PROVIDER, model, latency, reply.length());

            return LlmResponse.of(reply, PROVIDER, model, latency, request.promptChars());

        } catch (WebClientResponseException ex) {
            long latency = Instant.now().toEpochMilli() - start;
            int statusCode = ex.getStatusCode().value();
            String errorBody = ex.getResponseBodyAsString();
            log.error("[LlmGateway] ✖ {} call failed | provider={} model={} statusCode={} latencyMs={} errorBody={}",
                    PROVIDER, PROVIDER, model, statusCode, latency, errorBody);
            throw new LlmException("OpenRouter call failed with status " + statusCode + ": " + errorBody, ex);
        } catch (Exception ex) {
            long latency = Instant.now().toEpochMilli() - start;
            log.error("[LlmGateway] ✖ {} call failed | provider={} model={} latencyMs={} error={}",
                    PROVIDER, PROVIDER, model, latency, ex.getMessage(), ex);
            throw new LlmException("OpenRouter call failed: " + ex.getMessage(), ex);
        }
    }

    @Override public String providerName() { return PROVIDER; }
    @Override public String modelName()    { return model; }

    private String extractReply(OpenRouterChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new LlmException("OpenRouter returned an empty response or choices list");
        }
        Choice choice = response.getChoices().get(0);
        if (choice.getMessage() == null || choice.getMessage().getContent() == null) {
            return "";
        }
        return choice.getMessage().getContent();
    }

    // ── Internal DTOs ignoring all unknown OpenRouter fields ──────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenRouterChatResponse {
        private List<Choice> choices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }
}


