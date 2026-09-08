package com.example.videogen.service.visual;

import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

public interface VisualGenerationService {

    /**
     * Generates (or falls back to a placeholder for) the visual asset for a scene.
     */
    GenerationResult generate(Job job, Scene scene);
}
