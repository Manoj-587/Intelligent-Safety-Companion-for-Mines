package com.minecompanion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cors")
public class AppConfig {

    // ── AIML Flask API ─────────────────────────────────────────────────────────

    @Value("${aiml.api.base-url}")
    private String aimlBaseUrl;

    @Value("${aiml.api.connect-timeout-ms}")
    private int aimlConnectTimeoutMs;

    @Value("${aiml.api.read-timeout-ms}")
    private int aimlReadTimeoutMs;

    // ── CORS ───────────────────────────────────────────────────────────────────

    private List<String> allowedOrigins = List.of();

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    // ── Beans ──────────────────────────────────────────────────────────────────

    /**
     * WebClient pre-configured for the AIML Flask prediction API.
     * Injected into AimlApiClient.
     */
    @Bean("aimlWebClient")
    public WebClient aimlWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, aimlConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(aimlReadTimeoutMs));

        return WebClient.builder()
                .baseUrl(aimlBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Global CORS configuration.
     * Allows the React dev server to call the Spring Boot API during development.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.toArray(String[]::new))
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
