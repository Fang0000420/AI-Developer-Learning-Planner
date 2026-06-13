package com.aidevplanner.backend.goal;

import java.util.List;

public record GoalKnowledgePreferenceRequest(
        List<Long> preferredDocumentIds,
        String preferredScope,
        List<String> preferredCategories
) {
}
