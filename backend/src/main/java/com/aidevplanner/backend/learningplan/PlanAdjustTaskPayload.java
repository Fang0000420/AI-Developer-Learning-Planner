package com.aidevplanner.backend.learningplan;

public record PlanAdjustTaskPayload(
        Long id,
        Integer dayIndex,
        Integer taskOrder,
        String title,
        String description,
        Integer estimatedMinutes,
        String type,
        String deliverable,
        String priority,
        DailyTaskStatus status
) {
}
