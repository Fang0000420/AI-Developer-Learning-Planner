package com.aidevplanner.backend.learningplan;

import java.time.LocalDateTime;
import java.util.List;

public record LearningPlanResponse(
        Long id,
        Long goalId,
        Long userId,
        Long sourceAgentRunId,
        String planTitle,
        Integer durationDays,
        List<PlanDayResponse> days,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
