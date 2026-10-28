package com.example.videogen.service.assembly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

/**
 * Offline assembly stand-in (profile "stub"): writes an empty placeholder MP4
 * without invoking ffmpeg, so the pipeline is fully testable without it installed.
 */
@Service
@Profile("stub")
public class StubVideoAssemblyService implements VideoAssemblyService {

    private final VideogenProperties.Assembly properties;

    public StubVideoAssemblyService(VideogenProperties properties) {
        this.properties = properties.getAssembly();
    }

    @Override
    public GenerationResult assemble(Job job, List<Scene> scenes, String subtitlePath) {
        try {
            Path workDir = Path.of(properties.getWorkDir());
            Files.createDirectories(workDir);
            Path finalOutput = workDir.resolve(job.getId() + "-final.mp4");
            Files.write(finalOutput, new byte[0]);
            return GenerationResult.ok(finalOutput.toString(), Map.of("engine", "stub", "sceneCount", scenes.size()));
        } catch (IOException e) {
            return GenerationResult.failure("Stub assembly failed to write placeholder output: " + e.getMessage());
        }
    }
}
