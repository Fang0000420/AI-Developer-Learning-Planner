package com.aidevplanner.backend.learningplan;

import java.time.LocalDateTime;

public record LearningPlanSummaryResponse(
        Long id,
        Long goalId,
        Long userId,
        String planTitle,
        Integer durationDays,
        Integer dayCount,
        Integer taskCount,
        Integer totalEstimatedMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
