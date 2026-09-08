package com.example.videogen.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.videogen.config.VideogenProperties;
import com.example.videogen.exception.PipelineStageException;
import com.example.videogen.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Local-filesystem-backed storage. No AWS/S3 dependency is required for the
 * MVP; this keeps outputs under {storage.base-dir}/jobs/{jobId}/.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final String STAGE = "storage";
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final VideogenProperties.Storage properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public LocalStorageService(VideogenProperties properties) {
        this.properties = properties.getStorage();
    }

    @Override
    public String storeFinalOutput(Job job, String localFinalPath) {
        try {
            Path jobDir = jobDir(job.getId());
            Files.createDirectories(jobDir);
            Path source = Path.of(localFinalPath);
            Path destination = jobDir.resolve("output" + extensionOf(source));
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new PipelineStageException(STAGE, "Failed to store final output: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveJobMetadata(Job job) {
        try {
            Path jobDir = jobDir(job.getId());
            Files.createDirectories(jobDir);
            Path metadataFile = jobDir.resolve("job.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), job);
        } catch (IOException e) {
            // Metadata persistence is best-effort; do not fail the job over it.
            log.warn("Failed to persist metadata for job {}: {}", job.getId(), e.getMessage());
        }
    }

    private Path jobDir(String jobId) {
        return Path.of(properties.getBaseDir(), "jobs", jobId);
    }

    private static String extensionOf(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }
}
