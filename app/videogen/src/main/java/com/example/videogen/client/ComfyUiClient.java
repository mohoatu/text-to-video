package com.example.videogen.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.videogen.config.VideogenProperties;

/**
 * ComfyUI HTTP client: submits an API-format workflow graph, polls its
 * execution history, and downloads generated output files. Callers must
 * treat any failure as a signal to fall back to a placeholder asset.
 */
@Component
public class ComfyUiClient {

    private final RestClient restClient;
    private final VideogenProperties.Visual properties;

    public ComfyUiClient(VideogenProperties properties) {
        this.properties = properties.getVisual();
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(this.properties.getTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(this.properties.getGenerationTimeoutSeconds()));
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
     * Submits an API-format workflow graph ({nodeId: {class_type, inputs}}) and
     * returns the raw JSON response, which includes the assigned prompt_id.
     */
    public String submitPrompt(Map<String, Object> workflowGraph) {
        return restClient.post()
                .uri("/prompt")
                .body(Map.of("prompt", workflowGraph))
                .retrieve()
                .body(String.class);
    }

    /**
     * Returns the raw JSON history entry for a prompt_id, or "{}" while it is
     * still queued/running (ComfyUI only populates history once finished).
     */
    public String getHistory(String promptId) {
        return restClient.get()
                .uri("/history/{promptId}", promptId)
                .retrieve()
                .body(String.class);
    }

    /**
     * Downloads a generated output file's raw bytes via ComfyUI's /view endpoint.
     */
    public byte[] downloadOutput(String filename, String subfolder, String type) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/view")
                        .queryParam("filename", filename)
                        .queryParam("subfolder", subfolder == null ? "" : subfolder)
                        .queryParam("type", type)
                        .build())
                .retrieve()
                .body(byte[].class);
    }
}
