package com.example.videogen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    /**
     * A short topic/prompt, or a full long-form document/article — scene count
     * adapts to the content's length (see videogen.llm.min-scenes/max-scenes).
     * For file-based input (a single long document, or a "folder" of text
     * files) use POST /api/jobs/upload instead, which builds this field for
     * you from the uploaded file(s).
     */
    @NotBlank(message = "prompt must not be blank")
    @Size(max = 50_000, message = "prompt must be at most 50000 characters")
    private String prompt;

    /**
     * Drawing style for scene visuals: a preset key ("anime", "flat-comic",
     * "ink-wash"), a free-form English style description, or left blank to use
     * the configured default. See {@link com.example.videogen.service.visual.VisualStylePreset}.
     */
    private String style;

    @Min(value = 5, message = "durationTargetSeconds must be at least 5")
    @Max(value = 180, message = "durationTargetSeconds must be at most 180")
    private Integer durationTargetSeconds;
}
