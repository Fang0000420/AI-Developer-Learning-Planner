package com.aidevplanner.backend.progress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProgressSubmitRequest(
        @NotNull(message = "Plan id is required.")
        @Positive(message = "Plan id must be positive.")
        Long planId,

        @NotNull(message = "Day index is required.")
        @Positive(message = "Day index must be positive.")
        Integer dayIndex,

        @NotBlank(message = "User feedback is required.")
        @Size(max = 4000, message = "User feedback cannot exceed 4000 characters.")
        String userFeedback,

        List<@Positive(message = "Completed task id must be positive.") Long> completedTaskIds,

        List<@Positive(message = "Unfinished task id must be positive.") Long> unfinishedTaskIds,

        List<@Size(max = 500, message = "Blocker cannot exceed 500 characters.") String> blockers
) {
}
