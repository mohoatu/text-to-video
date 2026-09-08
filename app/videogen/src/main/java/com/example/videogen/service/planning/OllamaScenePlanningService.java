package com.example.videogen.service.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.client.OllamaClient;
import com.example.videogen.exception.PipelineStageException;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("!stub")
public class OllamaScenePlanningService implements ScenePlanningService {

    private static final String STAGE = "planning";

    private static final String SYSTEM_PROMPT = """
            Bạn là một đạo diễn kịch bản video ngắn tiếng Việt.
            Nhiệm vụ: chia nội dung người dùng cung cấp thành 3 đến 6 phân cảnh (scene) ngắn,
            mỗi phân cảnh có lời đọc (narrationText) tự nhiên, đúng ngữ pháp tiếng Việt,
            và mô tả hình ảnh minh họa (visualPrompt) bằng tiếng Việt cho phân cảnh đó.
            Ước tính thời lượng đọc (durationHint) tính bằng giây cho mỗi phân cảnh.
            """;

    /**
     * JSON Schema passed as Ollama's structured-outputs "format" field. This is
     * materially more reliable than instructing the model in prose to "return a
     * JSON array" — with the bare "json" format the model sometimes returns a
     * single object instead of an array of scenes.
     */
    private static final Object SCENES_SCHEMA = Map.of(
            "type", "array",
            "minItems", 1,
            "items", Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "sceneIndex", Map.of("type", "integer"),
                            "narrationText", Map.of("type", "string"),
                            "visualPrompt", Map.of("type", "string"),
                            "durationHint", Map.of("type", "number")
                    ),
                    "required", List.of("sceneIndex", "narrationText", "visualPrompt", "durationHint")
            )
    );

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaScenePlanningService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public List<Scene> plan(Job job) {
        String rawJson;
        try {
            rawJson = ollamaClient.generate(SYSTEM_PROMPT, job.getPrompt(), SCENES_SCHEMA);
        } catch (Exception e) {
            throw new PipelineStageException(STAGE, "Failed to reach local LLM (Ollama): " + e.getMessage(), e);
        }
        return parseAndValidate(job.getId(), rawJson);
    }

    private List<Scene> parseAndValidate(String jobId, String rawJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new PipelineStageException(STAGE, "LLM did not return valid JSON: " + e.getMessage(), e);
        }

        if (!root.isArray() || root.isEmpty()) {
            throw new PipelineStageException(STAGE, "LLM response must be a non-empty JSON array of scenes");
        }

        List<Scene> scenes = new ArrayList<>();
        int index = 0;
        for (JsonNode node : root) {
            String narrationText = textOrNull(node, "narrationText");
            String visualPrompt = textOrNull(node, "visualPrompt");
            if (narrationText == null || narrationText.isBlank()) {
                throw new PipelineStageException(STAGE, "Scene " + index + " is missing narrationText");
            }
            if (visualPrompt == null || visualPrompt.isBlank()) {
                throw new PipelineStageException(STAGE, "Scene " + index + " is missing visualPrompt");
            }
            Double durationHint = node.hasNonNull("durationHint") ? node.get("durationHint").asDouble() : null;

            scenes.add(Scene.builder()
                    .id(UUID.randomUUID().toString())
                    .jobId(jobId)
                    .sceneIndex(index)
                    .narrationText(narrationText)
                    .visualPrompt(visualPrompt)
                    .durationHint(durationHint)
                    .build());
            index++;
        }
        return scenes;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
