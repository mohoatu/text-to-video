package com.example.videogen.controller;

import java.net.URI;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.videogen.dto.CreateJobRequest;
import com.example.videogen.dto.JobOutputResponse;
import com.example.videogen.dto.JobResponse;
import com.example.videogen.model.Job;
import com.example.videogen.service.JobService;
import com.example.videogen.util.TextFileReader;

@RestController
@RequestMapping("/api/jobs")
public class VideoJobController {

    private final JobService jobService;
    private final Validator validator;

    public VideoJobController(JobService jobService, Validator validator) {
        this.jobService = jobService;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        Job job = jobService.createJob(request);
        JobResponse body = JobResponse.from(job);
        return ResponseEntity.created(URI.create("/api/jobs/" + job.getId())).body(body);
    }

    /**
     * Same as {@link #createJob}, but the source content comes from one or more
     * uploaded .txt files instead of an inline prompt string — for a long
     * article/document, or a "folder" of text files (select multiple files in
     * the upload) that together form the source text. Files are concatenated
     * in filename order, each preceded by its filename as a heading, before
     * being planned into scenes exactly like a normal prompt.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobResponse> createJobFromFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "durationTargetSeconds", required = false) Integer durationTargetSeconds) {
        String combinedText = TextFileReader.readAndCombine(files);

        CreateJobRequest request = CreateJobRequest.builder()
                .prompt(combinedText)
                .style(style)
                .durationTargetSeconds(durationTargetSeconds)
                .build();
        validate(request);

        Job job = jobService.createJob(request);
        JobResponse body = JobResponse.from(job);
        return ResponseEntity.created(URI.create("/api/jobs/" + job.getId())).body(body);
    }

    private void validate(CreateJobRequest request) {
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
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
