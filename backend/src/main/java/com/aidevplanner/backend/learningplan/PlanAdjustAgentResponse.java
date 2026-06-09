package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanAdjustAgentResponse(
        List<PlanAdjustTaskPayload> nextDayTasks,
        List<PlanMovedTaskResponse> movedTasks,
        List<PlanSplitTaskResponse> splitTasks,
        String reason
) {
}
