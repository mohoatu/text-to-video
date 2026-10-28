package com.example.videogen.exception;

/**
 * Thrown by a pipeline stage (planning, tts, visual, subtitle, assembly, storage)
 * so the orchestrator can record which stage failed on the job.
 */
public class PipelineStageException extends RuntimeException {

    private final String stage;

    public PipelineStageException(String stage, String message) {
        super(message);
        this.stage = stage;
    }

    public PipelineStageException(String stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }
}
