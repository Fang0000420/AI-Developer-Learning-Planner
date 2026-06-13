package com.aidevplanner.backend.progress;

import java.util.List;

public record ProgressReviewAgentResponse(
        List<String> completedTasks,
        List<String> unfinishedTasks,
        List<String> blockers,
        String impact,
        String suggestion,
        List<String> wins,
        List<String> nextFocus,
        String paceAdjustment,
        String confidence
) {
}
