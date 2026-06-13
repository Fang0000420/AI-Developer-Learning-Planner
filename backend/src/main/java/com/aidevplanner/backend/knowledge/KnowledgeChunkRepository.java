package com.aidevplanner.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    List<KnowledgeChunk> findByDocumentIdIn(Collection<Long> documentIds);
}
