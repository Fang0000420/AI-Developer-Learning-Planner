package com.aidevplanner.backend.knowledge;

import java.util.UUID;

public record KnowledgeUploadResponse(
        KnowledgeDocumentResponse document,
        UUID jobId
) {
}
