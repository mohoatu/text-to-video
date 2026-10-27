package com.example.videogen.service.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.videogen.config.VideogenProperties;

class VisualStylePresetTest {

    private final VideogenProperties.Visual defaults = new VideogenProperties.Visual();

    @Test
    void blankStyleHintFallsBackToConfiguredDefault() {
        VisualStylePreset.Resolved resolved = VisualStylePreset.resolve(null, defaults);

        assertThat(resolved.stylePrefix()).isEqualTo(defaults.getStylePrefix());
        assertThat(resolved.negativePrompt()).isEqualTo(defaults.getNegativePrompt());
    }

    @Test
    void recognizesKnownPresetKey() {
        VisualStylePreset.Resolved resolved = VisualStylePreset.resolve("ink-wash", defaults);

        assertThat(resolved.stylePrefix()).contains("ink wash");
        assertThat(resolved.negativePrompt()).contains("colorful");
    }

    @Test
    void presetMatchingIsCaseInsensitiveAndSupportsAliases() {
        VisualStylePreset.Resolved byAlias = VisualStylePreset.resolve("thuy-mac", defaults);
        VisualStylePreset.Resolved byUppercaseKey = VisualStylePreset.resolve("INK-WASH", defaults);

        assertThat(byAlias.stylePrefix()).isEqualTo(byUppercaseKey.stylePrefix());
    }

    @Test
    void unknownStyleHintIsTreatedAsCustomPrefix() {
        VisualStylePreset.Resolved resolved = VisualStylePreset.resolve("watercolor painting, soft pastel colors", defaults);

        assertThat(resolved.stylePrefix()).isEqualTo("watercolor painting, soft pastel colors");
        assertThat(resolved.negativePrompt()).isEqualTo(defaults.getNegativePrompt());
    }
}
