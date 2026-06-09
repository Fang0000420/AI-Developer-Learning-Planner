package com.aidevplanner.backend.skillgap;

import java.util.List;

public record SkillGapAnalyzeResponse(
        List<SkillGapResponse> skillGaps
) {
}
