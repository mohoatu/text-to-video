package com.example.videogen.dto;

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
public class JobOutputResponse {
    private String id;
    private JobStatus status;
    private String outputUrl;
    private int sceneCount;

    public static JobOutputResponse from(Job job) {
        return JobOutputResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .outputUrl(job.getOutputUrl())
                .sceneCount(job.getScenes() == null ? 0 : job.getScenes().size())
                .build();
    }
}
