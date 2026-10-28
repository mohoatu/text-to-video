package com.example.videogen.service.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.client.OllamaClient;
import com.example.videogen.config.VideogenProperties;
import com.example.videogen.exception.PipelineStageException;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("!stub")
public class OllamaScenePlanningService implements ScenePlanningService {

    private static final String STAGE = "planning";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là một đạo diễn kịch bản video tiếng Việt.
            Nhiệm vụ: đọc và hiểu nội dung người dùng cung cấp (có thể là một câu ngắn, hoặc
            một đoạn văn/bài viết dài), sau đó chia thành các phân cảnh (scene) để dựng thành
            video. Số lượng phân cảnh PHẢI phù hợp với độ dài và số lượng ý chính của nội dung:
            - Nội dung ngắn, một ý: chia thành khoảng %d phân cảnh.
            - Nội dung dài, nhiều đoạn/nhiều ý: chia theo từng đoạn/từng ý chính trong bài,
              mỗi phân cảnh nên bao quát một ý trọn vẹn — không dồn quá nhiều ý vào một phân
              cảnh, và không lặp lại ý đã nói ở phân cảnh khác.
            - Tối đa %d phân cảnh dù nội dung dài đến đâu; nếu bài viết có nhiều ý hơn mức này,
              hãy gộp các ý gần nhau lại thay vì bỏ sót nội dung quan trọng.
            - Giữ đúng trình tự nội dung gốc, không thêm tình tiết bịa đặt ngoài nội dung đã cho.

            Mỗi phân cảnh gồm:
            - narrationText: lời đọc bằng tiếng Việt, tự nhiên, đúng ngữ pháp, chuyển thể
              (không copy nguyên văn máy móc) từ phần nội dung tương ứng của phân cảnh đó.
            - visualPrompt: mô tả hình ảnh minh họa cho phân cảnh, BẮT BUỘC viết bằng
              TIẾNG ANH (vì sẽ được đưa thẳng vào một mô hình sinh ảnh chỉ hiểu tiếng Anh
              tốt). Mô tả phải CỤ THỂ, TRỰC QUAN: liệt kê các đối tượng, nhân vật, hành
              động, bối cảnh có thể vẽ được — tránh khái niệm trừu tượng (ví dụ: không viết
              "chứng minh sự trong sạch", hãy viết cụ thể nhân vật đang làm gì, đứng ở đâu,
              trông như thế nào). Dùng dạng liệt kê ngắn gọn, phân tách bằng dấu phẩy, tối đa
              khoảng 20 từ. Ví dụ: "a poor young man standing by a river holding a glowing
              gem, small wooden village houses in the background, daytime".
            - durationHint: thời lượng đọc ước tính, tính bằng giây.
            """;

    private final OllamaClient ollamaClient;
    private final VideogenProperties.Llm properties;
    private final Object scenesSchema;
    private final String systemPrompt;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaScenePlanningService(OllamaClient ollamaClient, VideogenProperties properties) {
        this.ollamaClient = ollamaClient;
        this.properties = properties.getLlm();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(this.properties.getMinScenes(), this.properties.getMaxScenes());
        this.scenesSchema = buildScenesSchema(this.properties.getMinScenes(), this.properties.getMaxScenes());
    }

    /**
     * JSON Schema passed as Ollama's structured-outputs "format" field. This is
     * materially more reliable than instructing the model in prose to "return a
     * JSON array" — with the bare "json" format the model sometimes returns a
     * single object instead of an array of scenes. minItems/maxItems bound the
     * scene count to stay consistent with the prose instructions above (and
     * with how long a generation run is acceptable: each scene costs real TTS
     * + visual generation time).
     */
    private static Object buildScenesSchema(int minScenes, int maxScenes) {
        return Map.of(
                "type", "array",
                "minItems", Math.max(1, minScenes),
                "maxItems", maxScenes,
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
    }

    @Override
    public List<Scene> plan(Job job) {
        String rawJson;
        try {
            rawJson = ollamaClient.generate(systemPrompt, job.getPrompt(), scenesSchema);
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
            if (index >= properties.getMaxScenes()) {
                break;
            }
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
