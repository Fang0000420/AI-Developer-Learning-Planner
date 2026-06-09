package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanSplitTaskResponse(
        Long sourceTaskId,
        String sourceTitle,
        List<PlanAdjustTaskPayload> parts,
        String reason
) {
}
