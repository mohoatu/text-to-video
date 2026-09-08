package com.example.videogen.service.planning;

import java.util.List;

import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

public interface ScenePlanningService {

    /**
     * Turns the job's prompt into an ordered list of scenes, each with
     * narration text and a visual prompt. Implementations must validate
     * their output before returning it.
     */
    List<Scene> plan(Job job);
}
