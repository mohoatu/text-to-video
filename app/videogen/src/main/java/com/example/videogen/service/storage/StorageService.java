package com.example.videogen.service.storage;

import com.example.videogen.model.Job;

public interface StorageService {

    /**
     * Moves/copies the final rendered file into durable local storage and
     * returns a resolvable URL/path for the job's outputUrl field.
     */
    String storeFinalOutput(Job job, String localFinalPath);

    /**
     * Persists a structured snapshot of the job's current state (for later
     * retrieval / debugging), independent of the in-memory job repository.
     */
    void saveJobMetadata(Job job);
}
