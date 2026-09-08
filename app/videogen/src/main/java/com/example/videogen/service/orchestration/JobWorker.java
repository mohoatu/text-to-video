package com.example.videogen.service.orchestration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single background worker thread that pulls job ids off the queue and runs
 * them through the orchestrator one at a time, so the API layer never blocks
 * on pipeline execution. Kept deliberately simple for the MVP (one worker,
 * no partitioning); scaling to multiple workers/queues is a later concern.
 */
@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final JobQueue jobQueue;
    private final VideoGenerationOrchestrator orchestrator;
    private Thread workerThread;
    private volatile boolean running = true;

    public JobWorker(JobQueue jobQueue, VideoGenerationOrchestrator orchestrator) {
        this.jobQueue = jobQueue;
        this.orchestrator = orchestrator;
    }

    @PostConstruct
    public void start() {
        workerThread = new Thread(this::loop, "videogen-job-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void loop() {
        while (running) {
            try {
                String jobId = jobQueue.take();
                orchestrator.process(jobId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // The orchestrator already handles/records per-job failures; this
                // guards the worker loop itself from dying on an unexpected error.
                log.error("Unexpected error in job worker loop: {}", e.getMessage(), e);
            }
        }
        log.info("Job worker thread stopped");
    }
}
