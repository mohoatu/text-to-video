package com.example.videogen.service.tts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

/**
 * Deterministic offline TTS stand-in (profile "stub"): writes an empty audio
 * placeholder instead of invoking piper, so the pipeline can run without it.
 */
@Service
@Profile("stub")
public class StubTtsService implements TextToSpeechService {

    private final VideogenProperties.Tts properties;

    public StubTtsService(VideogenProperties properties) {
        this.properties = properties.getTts();
    }

    @Override
    public GenerationResult synthesize(Job job, Scene scene) {
        try {
            Path workDir = Path.of(properties.getWorkDir());
            Files.createDirectories(workDir);
            Path outputFile = workDir.resolve(job.getId() + "-scene-" + scene.getSceneIndex() + ".wav");
            Files.write(outputFile, new byte[0]);
            return GenerationResult.ok(outputFile.toString(), Map.of("engine", "stub"));
        } catch (IOException e) {
            return GenerationResult.failure("stub TTS failed to write placeholder audio: " + e.getMessage());
        }
    }
}
