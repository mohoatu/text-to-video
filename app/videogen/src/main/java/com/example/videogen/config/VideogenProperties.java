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
