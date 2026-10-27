package com.example.videogen.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Probes a media file's duration via ffprobe. Used so scene segment length
 * (and subtitle timing) can be based on the narration audio's *actual*
 * synthesized length rather than the LLM's estimate — otherwise a scene
 * whose real audio runs longer (or shorter) than guessed either gets cut
 * off mid-sentence or lingers on a silent frame.
 */
public final class MediaDurationProbe {

    private static final Logger log = LoggerFactory.getLogger(MediaDurationProbe.class);

    private MediaDurationProbe() {
    }

    public static Optional<Double> probeSeconds(String ffprobeExecutable, String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobeExecutable, "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0",
                    filePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffprobe timed out probing duration of {}", filePath);
                return Optional.empty();
            }
            if (process.exitValue() != 0 || output.isBlank()) {
                log.warn("ffprobe failed to read duration of {} (exit={}): {}", filePath, process.exitValue(), output);
                return Optional.empty();
            }
            return Optional.of(Double.parseDouble(output));
        } catch (IOException | InterruptedException | NumberFormatException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to probe duration of {}: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }
}
