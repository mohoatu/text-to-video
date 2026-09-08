package com.example.videogen.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.videogen.config.VideogenProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin wrapper around Ollama's native REST API (/api/generate).
 * Deliberately avoids the Spring AI starter to keep the build self-contained
 * (no extra Spring Milestones repository required for this MVP).
 */
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final VideogenProperties.Llm properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaClient(VideogenProperties properties) {
        this.properties = properties.getLlm();
        this.restClient = RestClient.builder()
                .baseUrl(this.properties.getOllamaBaseUrl())
                .requestFactory(clientRequestFactory(this.properties.getTimeoutSeconds()))
                .build();
    }

    private static org.springframework.http.client.SimpleClientHttpRequestFactory clientRequestFactory(int timeoutSeconds) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }

    /**
     * Calls Ollama's generate endpoint with a system + user prompt and requests
     * JSON-formatted output. Returns the raw "response" text produced by the model.
     */
    public String generate(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, "json");
    }

    /**
     * Same as {@link #generate(String, String)} but with a JSON Schema passed as
     * the "format" field, which Ollama's structured-outputs support uses to
     * constrain the response shape (e.g. "must be a non-empty array of objects
     * with these fields"). This is materially more reliable than instructing
     * the model in prose to "return a JSON array" — the bare "json" format
     * still lets the model return a single object instead of an array.
     */
    public String generate(String systemPrompt, String userPrompt, Object formatSchema) {
        Map<String, Object> body = Map.of(
                "model", properties.getOllamaModel(),
                "system", systemPrompt,
                "prompt", userPrompt,
                "stream", false,
                "format", formatSchema
        );

        String rawResponse = restClient.post()
                .uri("/api/generate")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(rawResponse);
            JsonNode responseField = node.get("response");
            if (responseField == null) {
                throw new IllegalStateException("Ollama response missing 'response' field: " + rawResponse);
            }
            return responseField.asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Ollama response: " + e.getMessage(), e);
        }
    }
}
