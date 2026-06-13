package com.aidevplanner.backend.progress;

import java.util.List;

public record AdaptiveScheduleResult(
        String pacing,
        String reason,
        double recentCompletionRate,
        double recentBlockerAverage,
        int minuteAdjustmentPercent,
        List<Integer> affectedDayIndexes
) {
}
