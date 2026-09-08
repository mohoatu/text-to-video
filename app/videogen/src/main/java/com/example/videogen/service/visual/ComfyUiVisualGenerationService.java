package com.example.videogen.service.visual;

import java.nio.file.Path;
import java.util.Map;

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

/**
 * Sends the scene's visual prompt to a local ComfyUI instance. Since a full
 * ComfyUI node-graph workflow is deployment-specific and not something we can
 * assume exists locally, any failure (disabled, unreachable, workflow error)
 * falls back to a placeholder image so the rest of the pipeline still runs.
 */
@Service
@Profile("!stub")
public class ComfyUiVisualGenerationService implements VisualGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ComfyUiVisualGenerationService.class);

    private final ComfyUiClient comfyUiClient;
    private final VideogenProperties.Visual properties;

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

    private GenerationResult callComfyUi(Job job, Scene scene) {
        // A real ComfyUI workflow graph is required here and is environment-specific.
        // Submitting the prompt establishes connectivity is genuinely usable; actual
        // asset retrieval depends on the configured workflow's output node.
        comfyUiClient.submitPrompt(Map.of("visual_prompt", scene.getVisualPrompt()));
        throw new UnsupportedOperationException("No ComfyUI workflow graph configured; falling back to placeholder");
    }

    private GenerationResult generatePlaceholder(Job job, Scene scene) {
        try {
            Path outputFile = Path.of(properties.getWorkDir(),
                    job.getId() + "-scene-" + scene.getSceneIndex() + ".png");
            PlaceholderAssetGenerator.generatePlaceholderImage(outputFile, scene.getVisualPrompt());
            return GenerationResult.ok(outputFile.toString(), Map.of("engine", "placeholder", "fallback", true));
        } catch (Exception e) {
            return GenerationResult.failure("Failed to generate placeholder visual: " + e.getMessage());
        }
    }
}
