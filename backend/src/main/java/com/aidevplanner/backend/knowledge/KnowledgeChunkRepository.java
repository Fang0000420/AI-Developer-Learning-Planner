package com.aidevplanner.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
