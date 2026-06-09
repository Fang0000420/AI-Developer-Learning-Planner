package com.aidevplanner.backend.learningplan;

import jakarta.validation.constraints.NotNull;

public record LearningPlanUpdateRequest(
        @NotNull(message = "Plan status is required.")
        LearningPlanStatus status
) {
}
