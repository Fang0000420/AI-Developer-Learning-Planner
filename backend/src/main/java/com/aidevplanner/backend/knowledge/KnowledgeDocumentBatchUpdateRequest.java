package com.aidevplanner.backend.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record KnowledgeDocumentBatchUpdateRequest(
        @NotEmpty(message = "At least one knowledge document id is required.")
        List<Long> documentIds,
        Boolean enabled,
        KnowledgeDocumentScope scope,
        @Min(value = 1, message = "Retrieval priority must be between 1 and 5.")
        @Max(value = 5, message = "Retrieval priority must be between 1 and 5.")
        Integer retrievalPriority,
        KnowledgeSourceCategory sourceCategory,
        String groupName,
        List<String> tags
) {
}
