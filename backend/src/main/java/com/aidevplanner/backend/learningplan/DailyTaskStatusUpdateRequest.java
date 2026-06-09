package com.aidevplanner.backend.learningplan;

import jakarta.validation.constraints.NotNull;

public record DailyTaskStatusUpdateRequest(
        @NotNull(message = "Task status is required.")
        DailyTaskStatus status
) {
}
