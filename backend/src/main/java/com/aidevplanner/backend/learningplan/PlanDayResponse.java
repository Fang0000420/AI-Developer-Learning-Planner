package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanDayResponse(
        Integer dayIndex,
        String theme,
        Integer totalEstimatedMinutes,
        List<PlanTaskResponse> tasks
) {
}
