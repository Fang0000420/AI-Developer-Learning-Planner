package com.aidevplanner.backend.skillgap;

import java.time.LocalDateTime;
import java.util.List;

public record SkillGapAnalysisResponse(
        Long runId,
        Long goalId,
        List<SkillGapResponse> skillGaps,
        LocalDateTime createdAt
) {
}
