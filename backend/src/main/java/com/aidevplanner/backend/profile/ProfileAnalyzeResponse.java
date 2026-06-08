package com.aidevplanner.backend.profile;

import java.util.List;

public record ProfileAnalyzeResponse(
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        String recommendedDirection
) {
}
