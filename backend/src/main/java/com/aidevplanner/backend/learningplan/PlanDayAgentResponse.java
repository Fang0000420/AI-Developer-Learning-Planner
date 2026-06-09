package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanDayAgentResponse(
        Integer dayIndex,
        String theme,
        List<PlanTaskAgentResponse> tasks
) {
}
