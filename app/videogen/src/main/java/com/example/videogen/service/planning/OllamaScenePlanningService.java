package com.example.videogen.service.planning;

import java.util.ArrayList;
import java.util.List;
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
            Nhiệm vụ: chia nội dung người dùng cung cấp thành các phân cảnh (scene) ngắn.
            Chỉ trả lời bằng một JSON array hợp lệ, không thêm giải thích, không thêm markdown.
            Mỗi phần tử trong array phải có đúng các trường sau:
            - sceneIndex (số nguyên, bắt đầu từ 0)
            - narrationText (chuỗi tiếng Việt, lời đọc cho phân cảnh)
            - visualPrompt (chuỗi tiếng Việt, mô tả hình ảnh minh họa cho phân cảnh)
            - durationHint (số giây ước tính, kiểu số)
            Ví dụ định dạng:
            [{"sceneIndex":0,"narrationText":"...","visualPrompt":"...","durationHint":5}]
            """;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaScenePlanningService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public List<Scene> plan(Job job) {
        String rawJson;
        try {
            rawJson = ollamaClient.generate(SYSTEM_PROMPT, job.getPrompt());
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
            int sceneIndex = node.hasNonNull("sceneIndex") ? node.get("sceneIndex").asInt() : index;

            scenes.add(Scene.builder()
                    .id(UUID.randomUUID().toString())
                    .jobId(jobId)
                    .sceneIndex(sceneIndex)
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
