package com.aidevplanner.backend.learningplan;

import java.util.List;

public record PlanAdjustReviewPayload(
        List<String> completedTasks,
        List<String> unfinishedTasks,
        List<String> blockers,
        String impact,
        String suggestion
) {
}
