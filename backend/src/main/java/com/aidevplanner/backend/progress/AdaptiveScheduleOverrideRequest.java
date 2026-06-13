package com.aidevplanner.backend.progress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdaptiveScheduleOverrideRequest(
        @NotBlank(message = "Pacing is required.")
        @Pattern(
                regexp = "lighter|steady|stronger",
                message = "Pacing must be lighter, steady, or stronger."
        )
        String pacing,
        String reason
) {
}
