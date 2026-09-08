package com.example.videogen.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.videogen.config.VideogenProperties;

/**
 * Minimal ComfyUI HTTP client. A full ComfyUI integration requires posting a
 * complete node-graph workflow, which is deployment-specific; this client only
 * verifies the server is reachable and submits a prompt payload placeholder.
 * Callers must treat any failure as a signal to fall back to a placeholder asset.
 */
@Component
public class ComfyUiClient {

    private final RestClient restClient;
    private final VideogenProperties.Visual properties;

    public ComfyUiClient(VideogenProperties properties) {
        this.properties = properties.getVisual();
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(this.properties.getTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(this.properties.getTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(this.properties.getComfyUiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public boolean isReachable() {
        try {
            restClient.get().uri("/system_stats").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Submits a workflow payload and returns the raw JSON response from ComfyUI.
     * The caller is responsible for supplying a valid node-graph workflow.
     */
    public String submitPrompt(Map<String, Object> workflowPayload) {
        return restClient.post()
                .uri("/prompt")
                .body(Map.of("prompt", workflowPayload))
                .retrieve()
                .body(String.class);
    }
}
