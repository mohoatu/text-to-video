package com.example.videogen.repository;

import java.util.Optional;

import com.example.videogen.model.Job;

public interface JobRepository {
    Job save(Job job);

    Optional<Job> findById(String id);
}
