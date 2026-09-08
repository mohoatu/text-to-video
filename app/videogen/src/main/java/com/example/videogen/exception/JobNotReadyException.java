package com.example.videogen.exception;

import com.example.videogen.model.JobStatus;

public class JobNotReadyException extends RuntimeException {
    public JobNotReadyException(String jobId, JobStatus status) {
        super("Job " + jobId + " is not completed yet (current status: " + status + ")");
    }
}
