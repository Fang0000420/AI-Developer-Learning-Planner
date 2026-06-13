package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.goal.GoalKnowledgePreferenceResponse;

import java.util.List;

public record KnowledgeBasisResponse(
        String summary,
        GoalKnowledgePreferenceResponse preference,
        List<String> referencedDocumentTitles,
        List<String> knowledgeEvidence,
        List<KnowledgeBasisDocumentResponse> documents
) {
}
