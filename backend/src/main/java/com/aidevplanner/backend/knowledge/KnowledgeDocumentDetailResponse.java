package com.aidevplanner.backend.knowledge;

import java.time.LocalDateTime;

public record KnowledgeDocumentDetailResponse(
        Long id,
        Long userId,
        String scope,
        String sourceCategory,
        String groupName,
        java.util.List<String> tags,
        String title,
        String sourceLabel,
        String originalFileName,
        String mimeType,
        Long fileSizeBytes,
        String status,
        boolean enabled,
        Integer retrievalPriority,
        String summary,
        Long chunkCount,
        String previewText,
        LocalDateTime importedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static KnowledgeDocumentDetailResponse from(KnowledgeDocument document, long chunkCount) {
        String rawText = document.getRawText();
        String preview = rawText == null ? null : rawText.substring(0, Math.min(rawText.length(), 2000));
        return new KnowledgeDocumentDetailResponse(
                document.getId(),
                document.getUser().getId(),
                document.getScope().name(),
                document.getSourceCategory().name(),
                document.getGroupName(),
                document.getTags(),
                document.getTitle(),
                document.getSourceLabel(),
                document.getOriginalFileName(),
                document.getMimeType(),
                document.getFileSizeBytes(),
                document.getStatus().name(),
                document.isEnabled(),
                document.getRetrievalPriority(),
                document.getSummary(),
                chunkCount,
                preview,
                document.getImportedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
