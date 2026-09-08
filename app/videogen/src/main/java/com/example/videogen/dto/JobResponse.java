package com.example.videogen.dto;

import java.time.Instant;

import com.example.videogen.model.Job;
import com.example.videogen.model.JobStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private String id;
    private JobStatus status;
    private String prompt;
    private Instant createdAt;
    private Instant updatedAt;
    private String outputUrl;
    private String errorMessage;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .prompt(job.getPrompt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .outputUrl(job.getOutputUrl())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
