package com.example.videogen.service.visual;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.example.videogen.config.VideogenProperties;

/**
 * A named visual-art-style preset: the English prompt fragment prepended to
 * each scene's visual description, plus anything that should additionally be
 * kept out of frame for that style. Resolved per-job from
 * {@link com.example.videogen.model.Job#getStyleHint()} so the drawing style
 * is a request-time parameter, not just a global default.
 */
public record VisualStylePreset(String key, String stylePrefix, String extraNegativePrompt) {

    public static final String ANIME = "anime";
    public static final String FLAT_COMIC = "flat-comic";
    public static final String INK_WASH = "ink-wash";

    private static final Map<String, VisualStylePreset> PRESETS = buildPresets();

    private static Map<String, VisualStylePreset> buildPresets() {
        Map<String, VisualStylePreset> presets = new LinkedHashMap<>();

        register(presets, new VisualStylePreset(
                ANIME,
                "anime manga style illustration, like Naruto or Dragon Ball Z, cel shaded, "
                        + "bold ink outlines, vibrant anime colors, dynamic composition",
                "western comic"
        ), "naruto", "dragon-ball", "dragonball");

        register(presets, new VisualStylePreset(
                FLAT_COMIC,
                "simple flat cartoon illustration, children's storybook art style, "
                        + "clean bold outlines, flat colors, minimalist background",
                null
        ), "comic", "simple", "storybook", "flat");

        register(presets, new VisualStylePreset(
                INK_WASH,
                // "As simple as possible, ink wash (thuy mac) style" per request: figures
                // reduced to a handful of confident brush strokes, monochrome, lots of
                // empty space — but the negative prompt below still rules out anything
                // that would make the subject hard to read at a glance.
                "traditional East Asian ink wash painting, thuy mac sumi-e style, monochrome "
                        + "black ink on plain paper background, minimalist, as few brush strokes "
                        + "as possible, simple bold expressive linework, figures reduced to simple "
                        + "silhouettes, clear single focal subject, generous negative space, "
                        + "clean uncluttered composition, easy to recognize at a glance",
                "colorful, vibrant colors, multiple colors, bright colors, gradient, busy background, "
                        + "cluttered, intricate detail, photorealistic, 3d render"
        ), "thuy-mac", "thuymac", "thủy mặc", "sumi-e", "sumie", "ink"
        );

        return presets;
    }

    private static void register(Map<String, VisualStylePreset> presets, VisualStylePreset preset, String... aliases) {
        presets.put(preset.key(), preset);
        for (String alias : aliases) {
            presets.put(normalize(alias), preset);
        }
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves a job's style hint into an actual (stylePrefix, negativePrompt)
     * pair:
     * - blank/null -> the configured global default (unchanged behavior).
     * - matches a known preset key/alias (case-insensitive) -> that preset.
     * - anything else -> treated as a custom style prompt fragment supplied
     *   directly by the caller, combined with the default negative prompt.
     */
    public static Resolved resolve(String styleHint, VideogenProperties.Visual defaults) {
        if (styleHint == null || styleHint.isBlank()) {
            return new Resolved(defaults.getStylePrefix(), defaults.getNegativePrompt());
        }

        VisualStylePreset preset = PRESETS.get(normalize(styleHint));
        if (preset != null) {
            String negative = preset.extraNegativePrompt() == null
                    ? defaults.getNegativePrompt()
                    : defaults.getNegativePrompt() + ", " + preset.extraNegativePrompt();
            return new Resolved(preset.stylePrefix(), negative);
        }

        // Unrecognized value: treat as a free-form custom style description.
        return new Resolved(styleHint.trim(), defaults.getNegativePrompt());
    }

    public record Resolved(String stylePrefix, String negativePrompt) {
    }
}
