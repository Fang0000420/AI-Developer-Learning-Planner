package com.aidevplanner.backend.learningplan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LearningPlanGenerateRequest(
        @NotNull(message = "Goal id is required.")
        @Positive(message = "Goal id must be positive.")
        Long goalId
) {
}
