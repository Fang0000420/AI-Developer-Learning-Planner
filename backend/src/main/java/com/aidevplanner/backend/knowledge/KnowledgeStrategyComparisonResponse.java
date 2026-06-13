package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.goal.GoalKnowledgePreferenceResponse;

import java.util.List;

public record KnowledgeStrategyComparisonResponse(
        Long baseGoalId,
        String baseGoalTitle,
        Long compareGoalId,
        String compareGoalTitle,
        GoalKnowledgePreferenceResponse basePreference,
        GoalKnowledgePreferenceResponse comparePreference,
        List<String> differences,
        List<KnowledgeStrategyComparisonDocumentResponse> onlyInBase,
        List<KnowledgeStrategyComparisonDocumentResponse> onlyInCompare,
        List<KnowledgeStrategyComparisonDocumentResponse> sharedDocuments
) {
}
