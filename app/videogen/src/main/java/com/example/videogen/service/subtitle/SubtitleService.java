package com.example.videogen.service.subtitle;

import java.util.List;

import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

public interface SubtitleService {

    /**
     * Builds a subtitle file (SRT) covering all scenes in order and returns its path.
     */
    String generate(Job job, List<Scene> scenes);
}
