package com.aidevplanner.backend.learningplan;

public record LearningPlanVersionTaskChangeResponse(
        Integer dayIndex,
        String title,
        String changeType,
        Integer previousEstimatedMinutes,
        Integer currentEstimatedMinutes
) {
}
