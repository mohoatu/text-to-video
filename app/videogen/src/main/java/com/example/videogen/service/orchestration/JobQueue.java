package com.example.videogen.service.orchestration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;

/**
 * Simple in-memory job queue. Stands in for a real queue (e.g. Amazon SQS)
 * for the MVP; the interface it exposes (enqueue/take) is small enough to
 * swap for a real queue client later without touching callers.
 */
@Component
public class JobQueue {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public void enqueue(String jobId) {
        queue.add(jobId);
    }

    public String take() throws InterruptedException {
        return queue.take();
    }
}
