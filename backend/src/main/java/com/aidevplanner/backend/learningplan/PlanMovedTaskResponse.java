package com.aidevplanner.backend.learningplan;

public record PlanMovedTaskResponse(
        Long taskId,
        String title,
        Integer fromDayIndex,
        Integer toDayIndex,
        String reason
) {
}
