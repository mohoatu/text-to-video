package com.example.videogen.service.subtitle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.exception.PipelineStageException;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.example.videogen.util.DurationEstimator;

@Service
public class SrtSubtitleService implements SubtitleService {

    private static final String STAGE = "subtitle";

    private final VideogenProperties.Assembly properties;

    public SrtSubtitleService(VideogenProperties properties) {
        this.properties = properties.getAssembly();
    }

    @Override
    public String generate(Job job, List<Scene> scenes) {
        StringBuilder srt = new StringBuilder();
        double cursorSeconds = 0.0;
        int sequence = 1;

        for (Scene scene : scenes) {
            double duration = scene.getDurationHint() != null && scene.getDurationHint() > 0
                    ? scene.getDurationHint()
                    : DurationEstimator.estimateSeconds(scene.getNarrationText());

            double start = cursorSeconds;
            double end = cursorSeconds + duration;

            srt.append(sequence).append('\n')
                    .append(formatTimestamp(start)).append(" --> ").append(formatTimestamp(end)).append('\n')
                    .append(scene.getNarrationText().trim()).append('\n')
                    .append('\n');

            cursorSeconds = end;
            sequence++;
        }

        try {
            Path workDir = Path.of(properties.getWorkDir());
            Files.createDirectories(workDir);
            Path outputFile = workDir.resolve(job.getId() + ".srt");
            Files.writeString(outputFile, srt.toString());
            return outputFile.toString();
        } catch (IOException e) {
            throw new PipelineStageException(STAGE, "Failed to write subtitle file: " + e.getMessage(), e);
        }
    }

    private static String formatTimestamp(double totalSeconds) {
        long millis = Math.round(totalSeconds * 1000);
        long hours = millis / 3_600_000;
        millis %= 3_600_000;
        long minutes = millis / 60_000;
        millis %= 60_000;
        long seconds = millis / 1000;
        millis %= 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
}
