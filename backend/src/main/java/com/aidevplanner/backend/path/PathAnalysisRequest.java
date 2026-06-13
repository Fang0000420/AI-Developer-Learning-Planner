package com.aidevplanner.backend.path;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PathAnalysisRequest(
        @NotNull(message = "Goal id is required.")
        @Positive(message = "Goal id must be positive.")
        Long goalId
) {
}
