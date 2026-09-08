package com.example.videogen.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private String id;
    private JobStatus status;
    private String prompt;
    private String styleHint;
    private Integer durationTargetSeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private String outputUrl;
    private String errorMessage;
    private String failedStage;

    @Builder.Default
    private List<Scene> scenes = new ArrayList<>();
}
