package com.example.videogen.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResult {
    private boolean success;
    private String outputPath;
    private Map<String, Object> metadata;
    private String errorMessage;

    public static GenerationResult ok(String outputPath, Map<String, Object> metadata) {
        return GenerationResult.builder()
                .success(true)
                .outputPath(outputPath)
                .metadata(metadata)
                .build();
    }

    public static GenerationResult failure(String errorMessage) {
        return GenerationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
