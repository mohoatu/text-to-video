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

    @NotBlank(message = "prompt must not be blank")
    @Size(max = 2000, message = "prompt must be at most 2000 characters")
    private String prompt;

    private String style;

    @Min(value = 5, message = "durationTargetSeconds must be at least 5")
    @Max(value = 180, message = "durationTargetSeconds must be at most 180")
    private Integer durationTargetSeconds;
}
