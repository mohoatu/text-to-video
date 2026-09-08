package com.example.videogen.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.videogen.dto.CreateJobRequest;
import com.example.videogen.exception.JobNotFoundException;
import com.example.videogen.exception.JobNotReadyException;
import com.example.videogen.model.Job;
import com.example.videogen.model.JobStatus;
import com.example.videogen.repository.JobRepository;
import com.example.videogen.service.orchestration.JobQueue;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobQueue jobQueue;

    public JobService(JobRepository jobRepository, JobQueue jobQueue) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
    }

    public Job createJob(CreateJobRequest request) {
        Instant now = Instant.now();
        Job job = Job.builder()
                .id(UUID.randomUUID().toString())
                .status(JobStatus.PENDING)
                .prompt(request.getPrompt())
                .styleHint(request.getStyle())
                .durationTargetSeconds(request.getDurationTargetSeconds())
                .createdAt(now)
                .updatedAt(now)
                .build();

        jobRepository.save(job);
        jobQueue.enqueue(job.getId());
        return job;
    }

    public Job getJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public Job getCompletedJobOutput(String jobId) {
        Job job = getJob(jobId);
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new JobNotReadyException(jobId, job.getStatus());
        }
        return job;
    }
}
