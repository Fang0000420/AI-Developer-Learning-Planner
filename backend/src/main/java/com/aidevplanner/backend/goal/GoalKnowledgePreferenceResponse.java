package com.aidevplanner.backend.goal;

import java.util.List;

public record GoalKnowledgePreferenceResponse(
        Long goalId,
        List<Long> preferredDocumentIds,
        String preferredScope,
        List<String> preferredCategories
) {
}
