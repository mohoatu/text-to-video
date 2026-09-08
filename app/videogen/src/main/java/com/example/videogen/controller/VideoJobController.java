package com.example.videogen.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.videogen.dto.CreateJobRequest;
import com.example.videogen.dto.JobOutputResponse;
import com.example.videogen.dto.JobResponse;
import com.example.videogen.model.Job;
import com.example.videogen.service.JobService;

@RestController
@RequestMapping("/api/jobs")
public class VideoJobController {

    private final JobService jobService;

    public VideoJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        Job job = jobService.createJob(request);
        JobResponse body = JobResponse.from(job);
        return ResponseEntity.created(URI.create("/api/jobs/" + job.getId())).body(body);
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable String id) {
        return JobResponse.from(jobService.getJob(id));
    }

    @GetMapping("/{id}/output")
    public JobOutputResponse getJobOutput(@PathVariable String id) {
        return JobOutputResponse.from(jobService.getCompletedJobOutput(id));
    }
}
