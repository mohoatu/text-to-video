package com.example.videogen.service.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;
import com.example.videogen.util.DurationEstimator;

/**
 * Deterministic offline planner used when no local LLM is available
 * (activated with the "stub" profile). Splits the prompt into sentence-sized
 * scenes instead of calling Ollama.
 */
@Service
@Profile("stub")
public class StubScenePlanningService implements ScenePlanningService {

    @Override
    public List<Scene> plan(Job job) {
        String[] sentences = job.getPrompt().split("(?<=[.!?])\\s+");
        List<Scene> scenes = new ArrayList<>();
        int index = 0;
        for (String sentence : sentences) {
            String text = sentence.trim();
            if (text.isEmpty()) {
                continue;
            }
            scenes.add(Scene.builder()
                    .id(UUID.randomUUID().toString())
                    .jobId(job.getId())
                    .sceneIndex(index)
                    .narrationText(text)
                    .visualPrompt(text)
                    .durationHint(DurationEstimator.estimateSeconds(text))
                    .build());
            index++;
        }
        if (scenes.isEmpty()) {
            scenes.add(Scene.builder()
                    .id(UUID.randomUUID().toString())
                    .jobId(job.getId())
                    .sceneIndex(0)
                    .narrationText(job.getPrompt())
                    .visualPrompt(job.getPrompt())
                    .durationHint(DurationEstimator.estimateSeconds(job.getPrompt()))
                    .build());
        }
        return scenes;
    }
}
