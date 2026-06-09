package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanGenerateAgentResponse(
        String planTitle,
        Integer durationDays,
        List<PlanDayAgentResponse> days
) {
}
