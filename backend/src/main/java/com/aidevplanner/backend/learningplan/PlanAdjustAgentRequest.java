package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanAdjustAgentRequest(
        Long planId,
        Integer currentDayIndex,
        List<PlanAdjustDayPayload> currentPlan,
        List<PlanAdjustTaskPayload> todayTasks,
        PlanAdjustReviewPayload progressReview,
        List<PlanAdjustTaskPayload> unfinishedTasks,
        List<PlanAdjustTaskPayload> nextDayTasks,
        String responseLanguage
) {
}
