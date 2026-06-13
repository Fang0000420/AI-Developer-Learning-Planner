package com.aidevplanner.backend.progress;

import java.time.LocalDateTime;
import java.util.List;

public record AdaptiveScheduleOverrideSummary(
        String pacing,
        String reason,
        List<Integer> affectedDayIndexes,
        Integer anchorDayIndex,
        LocalDateTime appliedAt
) {
}
