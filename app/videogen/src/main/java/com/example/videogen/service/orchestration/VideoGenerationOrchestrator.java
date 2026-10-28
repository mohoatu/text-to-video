package com.example.videogen.service.orchestration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.exception.PipelineStageException;
import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.JobStatus;
import com.example.videogen.model.Scene;
import com.example.videogen.repository.JobRepository;
import com.example.videogen.service.assembly.VideoAssemblyService;
import com.example.videogen.service.planning.ScenePlanningService;
import com.example.videogen.service.storage.StorageService;
import com.example.videogen.service.subtitle.SubtitleService;
import com.example.videogen.service.tts.TextToSpeechService;
import com.example.videogen.service.visual.VisualGenerationService;
import com.example.videogen.util.MediaDurationProbe;
import com.example.videogen.util.RetryUtil;

/**
 * Coordinates the full pipeline for a single job: planning -> TTS -> visuals
 * -> subtitles -> assembly -> storage, updating job status/errorMessage/
 * failedStage as it goes. Designed to be called from a background worker
 * (see {@link JobWorker}) so the API layer never blocks on it.
 */
@Service
public class VideoGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationOrchestrator.class);

    private final JobRepository jobRepository;
    private final ScenePlanningService scenePlanningService;
    private final TextToSpeechService textToSpeechService;
    private final VisualGenerationService visualGenerationService;
    private final SubtitleService subtitleService;
    private final VideoAssemblyService videoAssemblyService;
    private final StorageService storageService;
    private final VideogenProperties properties;

    public VideoGenerationOrchestrator(JobRepository jobRepository,
                                        ScenePlanningService scenePlanningService,
                                        TextToSpeechService textToSpeechService,
                                        VisualGenerationService visualGenerationService,
                                        SubtitleService subtitleService,
                                        VideoAssemblyService videoAssemblyService,
                                        StorageService storageService,
                                        VideogenProperties properties) {
        this.jobRepository = jobRepository;
        this.scenePlanningService = scenePlanningService;
        this.textToSpeechService = textToSpeechService;
        this.visualGenerationService = visualGenerationService;
        this.subtitleService = subtitleService;
        this.videoAssemblyService = videoAssemblyService;
        this.storageService = storageService;
        this.properties = properties;
    }

    public void process(String jobId) {
        Optional<Job> maybeJob = jobRepository.findById(jobId);
        if (maybeJob.isEmpty()) {
            log.warn("Orchestrator received unknown job id {}", jobId);
            return;
        }
        Job job = maybeJob.get();

        MDC.put("jobId", jobId);
        try {
            transitionTo(job, JobStatus.PROCESSING, null, null);
            runPipeline(job);
        } catch (PipelineStageException e) {
            log.error("Job {} failed at stage '{}': {}", jobId, e.getStage(), e.getMessage());
            transitionTo(job, JobStatus.FAILED, e.getStage(), e.getMessage());
        } catch (Exception e) {
            log.error("Job {} failed with an unexpected error: {}", jobId, e.getMessage(), e);
            transitionTo(job, JobStatus.FAILED, "unknown", e.getMessage());
        } finally {
            MDC.remove("jobId");
        }
    }

    private void runPipeline(Job job) throws Exception {
        MDC.put("stage", "planning");
        List<Scene> scenes = new ArrayList<>(withRetry("planning", properties.getLlm().getMaxAttempts(),
                () -> scenePlanningService.plan(job)));
        appendOutroSceneIfEnabled(job, scenes);
        job.setScenes(scenes);
        log.info("Job {} planned {} scenes", job.getId(), scenes.size());

        MDC.put("stage", "tts");
        for (Scene scene : scenes) {
            GenerationResult result = withRetry("tts", properties.getTts().getMaxAttempts(),
                    () -> textToSpeechService.synthesize(job, scene));
            requireSuccess("tts", result, "scene " + scene.getSceneIndex());
            scene.setAudioFilePath(result.getOutputPath());
            correctDurationFromActualAudio(scene);
        }

        MDC.put("stage", "visual");
        for (Scene scene : scenes) {
            GenerationResult result = visualGenerationService.generate(job, scene);
            requireSuccess("visual", result, "scene " + scene.getSceneIndex());
            scene.setVisualFilePath(result.getOutputPath());
        }

        MDC.put("stage", "subtitle");
        String subtitlePath = subtitleService.generate(job, scenes);
        log.info("Job {} subtitle file generated at {}", job.getId(), subtitlePath);

        MDC.put("stage", "assembly");
        GenerationResult assemblyResult = videoAssemblyService.assemble(job, scenes, subtitlePath);
        requireSuccess("assembly", assemblyResult, "final video");

        MDC.put("stage", "storage");
        String outputUrl = storageService.storeFinalOutput(job, assemblyResult.getOutputPath());

        job.setOutputUrl(outputUrl);
        transitionTo(job, JobStatus.COMPLETED, null, null);
    }

    /**
     * Sets the scene's duration from the actual synthesized narration audio
     * (plus a little breathing room) rather than the LLM's estimate, so the
     * visual neither cuts off mid-sentence (estimate too short) nor lingers
     * oddly on a silent frame (estimate too long). Falls back to the existing
     * durationHint if ffprobe can't read the audio file for any reason.
     */
    private void correctDurationFromActualAudio(Scene scene) {
        VideogenProperties.Assembly assembly = properties.getAssembly();
        MediaDurationProbe.probeSeconds(assembly.getFfprobeExecutable(), scene.getAudioFilePath())
                .ifPresent(actualSeconds -> scene.setDurationHint(
                        Math.max(actualSeconds + assembly.getScenePaddingSeconds(), assembly.getMinSceneSeconds())));
    }

    /**
     * Appends a short, fixed "The End" scene so every job ends on a deliberate
     * beat instead of cutting off right as the last narration line finishes.
     * Runs through the same pipeline (TTS, visual, subtitle, assembly) as any
     * other scene, just with fixed content from configuration rather than the LLM.
     */
    private void appendOutroSceneIfEnabled(Job job, List<Scene> scenes) {
        VideogenProperties.Outro outro = properties.getOutro();
        if (!outro.isEnabled()) {
            return;
        }
        int nextIndex = scenes.size();
        scenes.add(Scene.builder()
                .id(UUID.randomUUID().toString())
                .jobId(job.getId())
                .sceneIndex(nextIndex)
                .narrationText(outro.getNarrationText())
                .visualPrompt(outro.getVisualPrompt())
                .durationHint(outro.getDurationSeconds())
                .build());
    }

    private <T> T withRetry(String stage, int maxAttempts, Callable<T> action) throws Exception {
        try {
            return RetryUtil.withRetry(stage, Math.max(1, maxAttempts), 500L, action);
        } catch (PipelineStageException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineStageException(stage, e.getMessage(), e);
        }
    }

    private static void requireSuccess(String stage, GenerationResult result, String context) {
        if (result == null || !result.isSuccess()) {
            String message = result == null ? "no result returned" : result.getErrorMessage();
            throw new PipelineStageException(stage, "Failed at " + context + ": " + message);
        }
    }

    private void transitionTo(Job job, JobStatus status, String failedStage, String errorMessage) {
        job.setStatus(status);
        job.setUpdatedAt(Instant.now());
        job.setFailedStage(failedStage);
        job.setErrorMessage(errorMessage);
        jobRepository.save(job);
        storageService.saveJobMetadata(job);
        log.info("Job {} transitioned to {}", job.getId(), status);
    }
}
