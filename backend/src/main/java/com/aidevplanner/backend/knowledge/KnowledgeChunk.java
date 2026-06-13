package com.aidevplanner.backend.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected KnowledgeChunk() {
    }

    public KnowledgeChunk(
            KnowledgeDocument document,
            Integer chunkIndex,
            String content,
            Integer characterCount
    ) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.characterCount = characterCount;
    }

    public Long getId() {
        return id;
    }

    public KnowledgeDocument getDocument() {
        return document;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
