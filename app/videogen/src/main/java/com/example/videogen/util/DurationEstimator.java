package com.example.videogen.util;

/**
 * Estimates narration duration when the LLM does not supply a durationHint,
 * using an average Vietnamese speaking rate of ~2.5 words/second.
 */
public final class DurationEstimator {

    private static final double WORDS_PER_SECOND = 2.5;
    private static final double MIN_SECONDS = 2.0;

    private DurationEstimator() {
    }

    public static double estimateSeconds(String narrationText) {
        if (narrationText == null || narrationText.isBlank()) {
            return MIN_SECONDS;
        }
        int words = narrationText.trim().split("\\s+").length;
        return Math.max(MIN_SECONDS, words / WORDS_PER_SECOND);
    }
}
