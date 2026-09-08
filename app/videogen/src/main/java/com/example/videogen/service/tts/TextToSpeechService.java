package com.example.videogen.service.tts;

import com.example.videogen.model.GenerationResult;
import com.example.videogen.model.Job;
import com.example.videogen.model.Scene;

public interface TextToSpeechService {

    /**
     * Synthesizes narration audio for a scene and returns the local file path
     * on success, or a failure result with an error message.
     */
    GenerationResult synthesize(Job job, Scene scene);
}
