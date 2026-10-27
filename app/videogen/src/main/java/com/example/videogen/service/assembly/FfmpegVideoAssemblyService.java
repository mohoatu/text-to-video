package com.example.videogen.service.assembly;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.example.videogen.util.DurationEstimator;

/**
 * Builds the final MP4 using the ffmpeg CLI:
 * 1) turn each scene's visual (a still image, or an animated .gif from
 *    ComfyUI/AnimateDiff, looped as needed) + narration audio into a
 *    fixed-duration segment,
 * 2) concatenate the segments in scene order,
 * 3) burn the SRT subtitle file into the concatenated video and fade
 *    video+audio to black over the last few seconds, so the ending is
 *    deliberate rather than an abrupt cut.
 * Requires the ffmpeg binary to be installed and on PATH (or configured explicitly).
 */
@Service
@Profile("!stub")
public class FfmpegVideoAssemblyService implements VideoAssemblyService {

    private final VideogenProperties.Assembly properties;
    private final double fadeOutSeconds;

    public FfmpegVideoAssemblyService(VideogenProperties properties) {
        this.properties = properties.getAssembly();
        this.fadeOutSeconds = properties.getOutro().getFadeOutSeconds();
    }

    @Override
    public GenerationResult assemble(Job job, List<Scene> scenes, String subtitlePath) {
        try {
            Path workDir = Path.of(properties.getWorkDir());
            Files.createDirectories(workDir);

            List<Path> segments = new ArrayList<>();
            double totalDuration = 0;
            for (Scene scene : scenes) {
                segments.add(buildSceneSegment(job, workDir, scene));
                totalDuration += sceneDuration(scene);
            }

            Path concatenated = concatenateSegments(job, workDir, segments);
            Path finalOutput = finalizeVideo(job, workDir, concatenated, subtitlePath, totalDuration);

            return GenerationResult.ok(finalOutput.toString(), Map.of("engine", "ffmpeg", "sceneCount", scenes.size()));
        } catch (AssemblyStepException e) {
            return GenerationResult.failure(e.getMessage());
        } catch (IOException e) {
            return GenerationResult.failure("Failed to prepare assembly work directory: " + e.getMessage());
        }
    }

    private static double sceneDuration(Scene scene) {
        return scene.getDurationHint() != null && scene.getDurationHint() > 0
                ? scene.getDurationHint()
                : DurationEstimator.estimateSeconds(scene.getNarrationText());
    }

    private Path buildSceneSegment(Job job, Path workDir, Scene scene) throws AssemblyStepException {
        Path segmentFile = workDir.resolve(job.getId() + "-segment-" + scene.getSceneIndex() + ".mp4");
        double duration = sceneDuration(scene);

        boolean isAnimatedClip = scene.getVisualFilePath().toLowerCase(java.util.Locale.ROOT).endsWith(".gif");

        List<String> visualInputArgs = isAnimatedClip
                // Animated clips (e.g. a ~2s AnimateDiff gif) are usually shorter than the
                // scene duration, so loop them indefinitely and let -t below trim to length.
                ? List.of("-stream_loop", "-1", "-i", scene.getVisualFilePath())
                // A still image has no inherent duration, so hold it on a single looped frame.
                : List.of("-loop", "1", "-i", scene.getVisualFilePath());

        List<String> command = new ArrayList<>();
        command.add(properties.getFfmpegExecutable());
        command.add("-y");
        command.addAll(visualInputArgs);
        command.addAll(List.of(
                "-i", scene.getAudioFilePath(),
                "-t", String.valueOf(duration),
                "-pix_fmt", "yuv420p",
                "-vf", "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2:color=black",
                "-c:v", "libx264",
                "-c:a", "aac",
                segmentFile.toString()));

        run(command, "assembly", "build segment for scene " + scene.getSceneIndex());
        return segmentFile;
    }

    private Path concatenateSegments(Job job, Path workDir, List<Path> segments) throws AssemblyStepException {
        Path listFile = workDir.resolve(job.getId() + "-concat.txt");
        try {
            StringBuilder sb = new StringBuilder();
            for (Path segment : segments) {
                sb.append("file '").append(segment.toAbsolutePath()).append("'\n");
            }
            Files.writeString(listFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssemblyStepException("Failed to write ffmpeg concat list: " + e.getMessage());
        }

        Path concatenated = workDir.resolve(job.getId() + "-concatenated.mp4");
        List<String> command = List.of(
                properties.getFfmpegExecutable(), "-y",
                "-f", "concat", "-safe", "0",
                "-i", listFile.toString(),
                "-c", "copy",
                concatenated.toString());

        run(command, "assembly", "concatenate scene segments");
        return concatenated;
    }

    /**
     * Burns in subtitles and, if configured, fades video+audio to black over the
     * last {@code fadeOutSeconds} so the video never just cuts off mid-frame.
     */
    private Path finalizeVideo(Job job, Path workDir, Path concatenated, String subtitlePath, double totalDuration)
            throws AssemblyStepException {
        Path finalOutput = workDir.resolve(job.getId() + "-final.mp4");
        String escapedSubtitlePath = subtitlePath.replace("\\", "/").replace(":", "\\:");

        StringBuilder videoFilter = new StringBuilder("subtitles='").append(escapedSubtitlePath).append('\'');
        List<String> audioFilterArgs;

        if (fadeOutSeconds > 0 && totalDuration > fadeOutSeconds) {
            double fadeStart = totalDuration - fadeOutSeconds;
            videoFilter.append(",fade=t=out:st=").append(fadeStart).append(":d=").append(fadeOutSeconds);
            audioFilterArgs = List.of("-af", "afade=t=out:st=" + fadeStart + ":d=" + fadeOutSeconds);
        } else {
            audioFilterArgs = List.of("-c:a", "copy");
        }

        List<String> command = new ArrayList<>(List.of(
                properties.getFfmpegExecutable(), "-y",
                "-i", concatenated.toString(),
                "-vf", videoFilter.toString()));
        command.addAll(audioFilterArgs);
        command.add(finalOutput.toString());

        run(command, "assembly", "burn subtitles and finalize video");
        return finalOutput;
    }

    private void run(List<String> command, String stage, String description) throws AssemblyStepException {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AssemblyStepException("ffmpeg timed out while trying to " + description);
            }
            if (process.exitValue() != 0) {
                throw new AssemblyStepException("ffmpeg failed to " + description + " (exit=" + process.exitValue()
                        + "): " + lastLines(output, 20));
            }
        } catch (IOException e) {
            throw new AssemblyStepException("ffmpeg executable not found or failed to start while trying to "
                    + description + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssemblyStepException("ffmpeg was interrupted while trying to " + description);
        }
    }

    private static String lastLines(String text, int maxLines) {
        String[] lines = text.split("\n");
        int start = Math.max(0, lines.length - maxLines);
        return String.join("\n", java.util.Arrays.copyOfRange(lines, start, lines.length));
    }

    private static class AssemblyStepException extends Exception {
        AssemblyStepException(String message) {
            super(message);
        }
    }
}
