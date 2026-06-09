package com.aidevplanner.backend.progress;

import java.util.List;

public record ProgressReviewAgentRequest(
        Integer dayIndex,
        List<ProgressReviewTaskPayload> todayTasks,
        String userFeedback,
        List<ProgressReviewTaskPayload> completedTasks,
        List<ProgressReviewTaskPayload> unfinishedTasks,
        List<String> blockers
) {
}
