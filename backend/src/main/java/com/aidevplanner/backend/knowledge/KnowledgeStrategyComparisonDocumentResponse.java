package com.aidevplanner.backend.knowledge;

import java.util.List;

public record KnowledgeStrategyComparisonDocumentResponse(
        Long documentId,
        String title,
        String scope,
        String sourceCategory,
        Integer baseScore,
        Integer compareScore,
        Integer scoreDelta,
        boolean selectedByBase,
        boolean selectedByCompare,
        List<String> tags
) {
}
