package com.aidevplanner.backend.knowledge;

import java.util.List;

public record KnowledgeRetrievalPreviewResponse(
        Long goalId,
        String goalTitle,
        Integer documentsReviewed,
        Integer matchedDocumentCount,
        Integer contextDocumentCount,
        List<String> rules,
        List<KnowledgeRetrievalPreviewMatchResponse> matches
) {
}
