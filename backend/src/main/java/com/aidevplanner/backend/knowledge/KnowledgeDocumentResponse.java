package com.aidevplanner.backend.knowledge;

import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        Long userId,
        String scope,
        String title,
        String sourceLabel,
        String originalFileName,
        String mimeType,
        Long fileSizeBytes,
        String status,
        boolean enabled,
        String summary,
        Long chunkCount,
        LocalDateTime importedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static KnowledgeDocumentResponse from(KnowledgeDocument document, long chunkCount) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getUser().getId(),
                document.getScope().name(),
                document.getTitle(),
                document.getSourceLabel(),
                document.getOriginalFileName(),
                document.getMimeType(),
                document.getFileSizeBytes(),
                document.getStatus().name(),
                document.isEnabled(),
                document.getSummary(),
                chunkCount,
                document.getImportedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
