package com.example.videogen.service.visual;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.example.videogen.util.PlaceholderAssetGenerator;

/**
 * Offline visual generator (profile "stub"): always produces a placeholder
 * image without attempting a ComfyUI network call.
 */
@Service
@Profile("stub")
public class StubVisualGenerationService implements VisualGenerationService {

    private final VideogenProperties.Visual properties;

    public StubVisualGenerationService(VideogenProperties properties) {
        this.properties = properties.getVisual();
    }

    @Override
    public GenerationResult generate(Job job, Scene scene) {
        try {
            Path outputFile = Path.of(properties.getWorkDir(),
                    job.getId() + "-scene-" + scene.getSceneIndex() + ".png");
            PlaceholderAssetGenerator.generatePlaceholderImage(outputFile, scene.getNarrationText());
            return GenerationResult.ok(outputFile.toString(), Map.of("engine", "stub"));
        } catch (Exception e) {
            return GenerationResult.failure("Stub visual generation failed: " + e.getMessage());
        }
    }
}
