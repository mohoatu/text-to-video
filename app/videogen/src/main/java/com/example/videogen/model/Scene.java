package com.example.videogen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scene {
    private String id;
    private String jobId;
    private int sceneIndex;
    private String narrationText;
    private String visualPrompt;
    private Double durationHint;
    private String voiceStyleHint;
    private String audioFilePath;
    private String visualFilePath;
}
