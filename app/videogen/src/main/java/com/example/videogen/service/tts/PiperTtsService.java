package com.example.videogen.service.tts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

/**
 * Invokes the local "piper" TTS CLI to synthesize Vietnamese narration audio.
 * Requires piper (and a Vietnamese voice model) to be installed locally;
 * see https://github.com/rhasspy/piper for setup.
 */
@Service
@Profile("!stub")
public class PiperTtsService implements TextToSpeechService {

    private static final Logger log = LoggerFactory.getLogger(PiperTtsService.class);

    private final VideogenProperties.Tts properties;

    public PiperTtsService(VideogenProperties properties) {
        this.properties = properties.getTts();
    }

    @Override
    public GenerationResult synthesize(Job job, Scene scene) {
        try {
            Path workDir = Path.of(properties.getWorkDir());
            Files.createDirectories(workDir);
            Path outputFile = workDir.resolve(job.getId() + "-scene-" + scene.getSceneIndex() + ".wav");

            ProcessBuilder pb = new ProcessBuilder(
                    properties.getPiperExecutable(),
                    "--model", properties.getPiperModel(),
                    "--length-scale", String.valueOf(properties.getLengthScale()),
                    "--output_file", outputFile.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            process.getOutputStream().write(scene.getNarrationText().getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return GenerationResult.failure("piper TTS timed out for scene " + scene.getSceneIndex());
            }
            if (process.exitValue() != 0 || !Files.exists(outputFile)) {
                return GenerationResult.failure("piper TTS failed (exit=" + process.exitValue() + "): " + log);
            }

            return GenerationResult.ok(outputFile.toString(), Map.of("engine", "piper"));
        } catch (IOException e) {
            PiperTtsService.log.warn("piper executable not available: {}", e.getMessage());
            return GenerationResult.failure("piper executable not found or failed to start: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GenerationResult.failure("piper TTS interrupted: " + e.getMessage());
        }
    }
}
