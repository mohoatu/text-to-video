package com.example.videogen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "videogen")
public class VideogenProperties {

    private final Llm llm = new Llm();
    private final Tts tts = new Tts();
    private final Visual visual = new Visual();
    private final Assembly assembly = new Assembly();
    private final Storage storage = new Storage();

    @Data
    public static class Llm {
        private String ollamaBaseUrl = "http://localhost:11434";
        private String ollamaModel = "qwen2.5:7b";
        private int timeoutSeconds = 60;
        private int maxAttempts = 2;
    }

    @Data
    public static class Tts {
        private String piperExecutable = "piper";
        private String piperModel = "";
        private String workDir = "./data/work/audio";
        private int maxAttempts = 2;
    }

    @Data
    public static class Visual {
        private String comfyUiBaseUrl = "http://127.0.0.1:8188";
        private boolean enabled = false;
        private String workDir = "./data/work/visual";
        private int timeoutSeconds = 30;

        // Standard SD1.5 txt2img workflow (CheckpointLoaderSimple -> KSampler ->
        // SaveImage; no custom nodes required), prompted toward a simple flat
        // comic/storybook illustration style rather than photorealism. A plain
        // SD1.5 checkpoint without a motion module follows the scene's visual
        // prompt far more faithfully than AnimateDiff did, and a still image
        // takes ~30s on M1 Pro MPS instead of several minutes per scene.
        private String checkpointName = "v1-5-pruned-emaonly.safetensors";
        private String stylePrefix = "simple flat cartoon illustration, children's storybook art style, "
                + "clean bold outlines, flat colors, minimalist background";
        private String negativePrompt = "photorealistic, realistic, 3d render, photography, blurry, "
                + "low quality, watermark, text, signature, extra limbs, deformed, noisy, grainy";
        private int imageWidth = 768;
        private int imageHeight = 512;
        private int steps = 25;
        private double cfg = 9.0;
        private int generationTimeoutSeconds = 180;
        private long pollIntervalMillis = 1000;
    }

    @Data
    public static class Assembly {
        private String ffmpegExecutable = "ffmpeg";
        private String workDir = "./data/work/assembly";
    }

    @Data
    public static class Storage {
        private String baseDir = "./data";
    }
}
