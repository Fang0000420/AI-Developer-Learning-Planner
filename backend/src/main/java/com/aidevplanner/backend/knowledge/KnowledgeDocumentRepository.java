package com.aidevplanner.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<KnowledgeDocument> findByIdAndUserId(Long id, Long userId);
}
