package com.example.videogen.service.visual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.client.ComfyUiClient;
import com.example.videogen.config.VideogenProperties;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.example.videogen.util.PlaceholderAssetGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generates an illustration still image per scene using a local ComfyUI
 * instance running a standard Stable Diffusion txt2img workflow
 * (CheckpointLoaderSimple -> KSampler -> SaveImage; no custom nodes). The
 * scene's visual prompt is wrapped with a drawing-style prefix (see
 * {@link VisualStylePreset} — resolved from the job's styleHint, i.e. the
 * "style" field on the create-job request, falling back to the configured
 * default) so output stays illustrative rather than attempting (and usually
 * failing at) photorealism. Any failure (disabled, unreachable, workflow
 * error, timeout) falls back to a plain placeholder image so the rest of the
 * pipeline still runs.
 */
@Service
@Profile("!stub")
public class ComfyUiVisualGenerationService implements VisualGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ComfyUiVisualGenerationService.class);

    private final ComfyUiClient comfyUiClient;
    private final VideogenProperties.Visual properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ComfyUiVisualGenerationService(ComfyUiClient comfyUiClient, VideogenProperties properties) {
        this.comfyUiClient = comfyUiClient;
        this.properties = properties.getVisual();
    }

    @Override
    public GenerationResult generate(Job job, Scene scene) {
        if (properties.isEnabled() && comfyUiClient.isReachable()) {
            try {
                return callComfyUi(job, scene);
            } catch (Exception e) {
                log.warn("ComfyUI generation failed for job {} scene {}, falling back to placeholder: {}",
                        job.getId(), scene.getSceneIndex(), e.getMessage());
            }
        } else {
            log.info("ComfyUI disabled or unreachable, using placeholder visual for job {} scene {}",
                    job.getId(), scene.getSceneIndex());
        }
        return generatePlaceholder(job, scene);
    }

    private GenerationResult callComfyUi(Job job, Scene scene) throws Exception {
        Map<String, Object> workflow = buildTxt2ImgWorkflow(scene.getVisualPrompt(), job.getStyleHint());

        String submitResponse = comfyUiClient.submitPrompt(workflow);
        JsonNode submitNode = objectMapper.readTree(submitResponse);
        JsonNode promptIdNode = submitNode.get("prompt_id");
        if (promptIdNode == null) {
            JsonNode nodeErrors = submitNode.get("node_errors");
            throw new IllegalStateException("ComfyUI rejected the workflow: " + nodeErrors);
        }
        String promptId = promptIdNode.asText();
        log.info("Submitted ComfyUI txt2img workflow for job {} scene {}: prompt_id={}",
                job.getId(), scene.getSceneIndex(), promptId);

        JsonNode historyEntry = pollUntilComplete(promptId);
        OutputFile outputFile = extractOutputFile(historyEntry);

        byte[] bytes = comfyUiClient.downloadOutput(outputFile.filename(), outputFile.subfolder(), outputFile.type());
        Path destination = Path.of(properties.getWorkDir(),
                job.getId() + "-scene-" + scene.getSceneIndex() + ".png");
        Files.createDirectories(destination.getParent());
        Files.write(destination, bytes);

        return GenerationResult.ok(destination.toString(), Map.of("engine", "comfyui-sd15", "promptId", promptId));
    }

    private Map<String, Object> buildTxt2ImgWorkflow(String visualPrompt, String styleHint) {
        long seed = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        VisualStylePreset.Resolved style = VisualStylePreset.resolve(styleHint, properties);
        String positivePrompt = style.stylePrefix() + ", " + visualPrompt;

        return Map.of(
                "4", Map.of(
                        "class_type", "CheckpointLoaderSimple",
                        "inputs", Map.of("ckpt_name", properties.getCheckpointName())),
                "6", Map.of(
                        "class_type", "CLIPTextEncode",
                        "inputs", Map.of("text", positivePrompt, "clip", java.util.List.of("4", 1))),
                "7", Map.of(
                        "class_type", "CLIPTextEncode",
                        "inputs", Map.of("text", style.negativePrompt(), "clip", java.util.List.of("4", 1))),
                "20", Map.of(
                        "class_type", "EmptyLatentImage",
                        "inputs", Map.of(
                                "width", properties.getImageWidth(),
                                "height", properties.getImageHeight(),
                                "batch_size", 1)),
                "3", Map.of(
                        "class_type", "KSampler",
                        "inputs", mapOf(
                                "model", java.util.List.of("4", 0),
                                "seed", seed,
                                "steps", properties.getSteps(),
                                "cfg", properties.getCfg(),
                                "sampler_name", "euler",
                                "scheduler", "normal",
                                "positive", java.util.List.of("6", 0),
                                "negative", java.util.List.of("7", 0),
                                "latent_image", java.util.List.of("20", 0),
                                "denoise", 1.0)),
                "8", Map.of(
                        "class_type", "VAEDecode",
                        "inputs", Map.of("samples", java.util.List.of("3", 0), "vae", java.util.List.of("4", 2))),
                "9", Map.of(
                        "class_type", "SaveImage",
                        "inputs", Map.of("images", java.util.List.of("8", 0), "filename_prefix", "videogen"))
        );
    }

    /** Map.of() caps out at 10 entries; KSampler's inputs need more. */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private JsonNode pollUntilComplete(String promptId) throws Exception {
        long deadline = System.currentTimeMillis() + properties.getGenerationTimeoutSeconds() * 1000L;
        while (System.currentTimeMillis() < deadline) {
            String historyResponse = comfyUiClient.getHistory(promptId);
            JsonNode history = objectMapper.readTree(historyResponse);
            JsonNode entry = history.get(promptId);
            if (entry != null) {
                JsonNode status = entry.get("status");
                String statusStr = status != null && status.hasNonNull("status_str")
                        ? status.get("status_str").asText() : null;
                if ("error".equals(statusStr)) {
                    throw new IllegalStateException("ComfyUI workflow execution failed: " + status);
                }
                if (status != null && status.hasNonNull("completed") && status.get("completed").asBoolean()) {
                    return entry;
                }
            }
            Thread.sleep(properties.getPollIntervalMillis());
        }
        throw new IllegalStateException("ComfyUI generation timed out after "
                + properties.getGenerationTimeoutSeconds() + "s for prompt " + promptId);
    }

    private record OutputFile(String filename, String subfolder, String type) {
    }

    private OutputFile extractOutputFile(JsonNode historyEntry) {
        JsonNode outputs = historyEntry.get("outputs");
        if (outputs != null) {
            for (JsonNode nodeOutput : outputs) {
                JsonNode images = nodeOutput.get("images");
                if (images != null && images.isArray() && !images.isEmpty()) {
                    JsonNode first = images.get(0);
                    return new OutputFile(
                            first.get("filename").asText(),
                            first.hasNonNull("subfolder") ? first.get("subfolder").asText() : "",
                            first.hasNonNull("type") ? first.get("type").asText() : "output");
                }
            }
        }
        throw new IllegalStateException("ComfyUI history entry had no 'images' output: " + historyEntry);
    }

    private GenerationResult generatePlaceholder(Job job, Scene scene) {
        try {
            Path outputFile = Path.of(properties.getWorkDir(),
                    job.getId() + "-scene-" + scene.getSceneIndex() + ".png");
            // Caption with the (Vietnamese) narration, not visualPrompt: visualPrompt is
            // now written in English for the image model and isn't meant for viewers.
            PlaceholderAssetGenerator.generatePlaceholderImage(outputFile, scene.getNarrationText());
            return GenerationResult.ok(outputFile.toString(), Map.of("engine", "placeholder", "fallback", true));
        } catch (Exception e) {
            return GenerationResult.failure("Failed to generate placeholder visual: " + e.getMessage());
        }
    }
}
