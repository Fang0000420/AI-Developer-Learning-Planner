package com.aidevplanner.backend.goal;

import java.util.List;

public record GoalKnowledgePreference(
        List<Long> preferredDocumentIds,
        String preferredScope,
        List<String> preferredCategories
) {
}
