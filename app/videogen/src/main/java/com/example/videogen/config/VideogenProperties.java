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
    private final Outro outro = new Outro();

    @Data
    public static class Llm {
        private String ollamaBaseUrl = "http://localhost:11434";
        private String ollamaModel = "qwen2.5:7b";
        private int timeoutSeconds = 180;
        private int maxAttempts = 2;
        // Ollama's context window (num_ctx) for the planning call. The default
        // model-level context is often too small for a long source document
        // (a full paragraph/article), so this is set generously; qwen2.5:14b
        // supports up to 32768 natively.
        private int contextLength = 8192;
        // Scene count is adaptive to the input's length rather than fixed, bounded
        // by these so a very long document doesn't produce an unbounded number of
        // scenes (each costs real TTS + visual generation time).
        private int minScenes = 3;
        private int maxScenes = 20;
    }

    @Data
    public static class Tts {
        private String piperExecutable = "piper";
        private String piperModel = "";
        private String workDir = "./data/work/audio";
        private int maxAttempts = 2;
        // piper's --length-scale: 1.0 is the voice's natural pace; higher values
        // slow it down (e.g. 1.2 = ~20% slower). Vietnamese narration tends to be
        // easier to follow a bit slower than the model's default pace.
        private double lengthScale = 1.15;
    }

    @Data
    public static class Visual {
        private String comfyUiBaseUrl = "http://127.0.0.1:8188";
        private boolean enabled = false;
        private String workDir = "./data/work/visual";
        private int timeoutSeconds = 30;

        // Standard SD1.5 txt2img workflow (CheckpointLoaderSimple -> KSampler ->
        // SaveImage; no custom nodes required). Default style is traditional ink
        // wash (thuy mac/sumi-e) — see VisualStylePreset.INK_WASH, which this
        // mirrors: as simple/minimal as possible while staying immediately
        // legible. A job can override this per request via the "style" field
        // (preset key or free-form text); see VisualStylePreset. A plain SD1.5
        // checkpoint without a motion module follows the scene's visual prompt
        // far more faithfully than AnimateDiff did, and a still image takes
        // ~30s on M1 Pro MPS instead of several minutes per scene.
        private String checkpointName = "v1-5-pruned-emaonly.safetensors";
        private String stylePrefix = "traditional East Asian ink wash painting, thuy mac sumi-e style, "
                + "monochrome black ink on plain paper background, minimalist, as few brush strokes as "
                + "possible, simple bold expressive linework, figures reduced to simple silhouettes, "
                + "clear single focal subject, generous negative space, clean uncluttered composition, "
                + "easy to recognize at a glance";
        private String negativePrompt = "colorful, vibrant colors, multiple colors, bright colors, gradient, "
                + "busy background, cluttered, intricate detail, photorealistic, realistic, 3d render, "
                + "photography, western comic, blurry, low quality, watermark, text, signature, extra limbs, "
                + "deformed, noisy, grainy";
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
        private String ffprobeExecutable = "ffprobe";
        private String workDir = "./data/work/assembly";
        // After TTS, each scene's duration is corrected to the *actual* synthesized
        // audio length (not the LLM's estimate) plus this much breathing room, so a
        // scene never gets cut off mid-sentence or lingers oddly past the narration.
        // Falls back to the LLM's durationHint/text-based estimate if ffprobe fails.
        private double scenePaddingSeconds = 0.4;
        private double minSceneSeconds = 1.5;
    }

    @Data
    public static class Storage {
        private String baseDir = "./data";
    }

    @Data
    public static class Outro {
        // Appended as an extra scene after planning so every video has a clear,
        // deliberate ending instead of cutting off right as the last line of
        // narration finishes.
        private boolean enabled = true;
        private String narrationText = "Hết.";
        private String visualPrompt = "\"THE END\" title card, bold stylized text, simple flat "
                + "illustration background, clean design";
        private double durationSeconds = 2.5;
        // Final fade-to-black applied to the whole assembled video, independent
        // of the outro scene, so the last moment never looks like an abrupt cut.
        private double fadeOutSeconds = 1.0;
    }
}
