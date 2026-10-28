package com.example.videogen.service.assembly;

import java.util.List;

import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

public interface VideoAssemblyService {

    /**
     * Combines each scene's audio + visual into per-scene segments, concatenates
     * them, and burns in the subtitle file, producing a final MP4.
     */
    GenerationResult assemble(Job job, List<Scene> scenes, String subtitlePath);
}
