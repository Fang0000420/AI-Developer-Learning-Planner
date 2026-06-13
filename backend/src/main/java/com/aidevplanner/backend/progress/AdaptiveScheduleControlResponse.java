package com.aidevplanner.backend.progress;

import java.util.List;

public record AdaptiveScheduleControlResponse(
        AdaptiveScheduleResult latestAutomatic,
        AdaptiveScheduleOverrideSummary activeOverride,
        List<String> evidence
) {
}
