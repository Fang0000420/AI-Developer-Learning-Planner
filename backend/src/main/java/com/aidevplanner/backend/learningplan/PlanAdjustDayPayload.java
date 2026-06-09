package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanAdjustDayPayload(
        Integer dayIndex,
        String theme,
        List<PlanAdjustTaskPayload> tasks
) {
}
