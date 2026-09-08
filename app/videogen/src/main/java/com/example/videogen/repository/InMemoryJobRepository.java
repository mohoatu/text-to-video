package com.example.videogen.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.videogen.model.Job;

@Repository
public class InMemoryJobRepository implements JobRepository {

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    @Override
    public Job save(Job job) {
        jobs.put(job.getId(), job);
        return job;
    }

    @Override
    public Optional<Job> findById(String id) {
        return Optional.ofNullable(jobs.get(id));
    }
}
