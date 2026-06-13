package com.aidevplanner.backend.knowledge;

import java.util.List;

public record KnowledgeRetrievalPreviewMatchResponse(
        Long documentId,
        String title,
        String scope,
        String sourceCategory,
        Integer retrievalPriority,
        String groupName,
        List<String> tags,
        Integer score,
        boolean selectedForContext,
        List<String> reasons,
        List<String> excerpts
) {
}
