package com.aidevplanner.backend.knowledge;

import java.util.List;

public record KnowledgeBasisDocumentResponse(
        Long documentId,
        String title,
        String scope,
        String sourceCategory,
        boolean selectedForContext,
        List<String> reasons
) {
}
