package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeDocumentScope scope;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_category", nullable = false, length = 30)
    private KnowledgeSourceCategory sourceCategory = KnowledgeSourceCategory.NOTE;

    @Column(name = "group_name", length = 120)
    private String groupName;

    @Column(name = "tags_text", columnDefinition = "TEXT")
    private String tagsText;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeDocumentStatus status;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "retrieval_priority", nullable = false)
    private Integer retrievalPriority = 3;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected KnowledgeDocument() {
    }

    public KnowledgeDocument(
            User user,
            KnowledgeDocumentScope scope,
            String title,
            String sourceLabel,
            String originalFileName,
            String mimeType,
            Long fileSizeBytes,
            String storagePath
    ) {
        this.user = user;
        this.scope = scope;
        this.title = title;
        this.sourceLabel = sourceLabel;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.storagePath = storagePath;
        this.status = KnowledgeDocumentStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = KnowledgeDocumentStatus.PENDING;
        }
        if (sourceCategory == null) {
            sourceCategory = KnowledgeSourceCategory.NOTE;
        }
        if (retrievalPriority == null) {
            retrievalPriority = 3;
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public KnowledgeDocumentScope getScope() {
        return scope;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public KnowledgeSourceCategory getSourceCategory() {
        return sourceCategory;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<String> getTags() {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagsText.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getRawText() {
        return rawText;
    }

    public String getSummary() {
        return summary;
    }

    public KnowledgeDocumentStatus getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public Integer getRetrievalPriority() {
        return retrievalPriority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        this.status = KnowledgeDocumentStatus.PROCESSING;
        this.summary = null;
        this.importedAt = null;
    }

    public void markReady(String rawText, String summary) {
        this.status = KnowledgeDocumentStatus.READY;
        this.rawText = rawText;
        this.summary = summary;
        this.importedAt = LocalDateTime.now();
    }

    public void markFailed(String message) {
        this.status = KnowledgeDocumentStatus.FAILED;
        this.summary = message;
        this.importedAt = null;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setScope(KnowledgeDocumentScope scope) {
        this.scope = scope == null ? KnowledgeDocumentScope.PERSONAL : scope;
    }

    public void setSourceCategory(KnowledgeSourceCategory sourceCategory) {
        this.sourceCategory = sourceCategory == null ? KnowledgeSourceCategory.NOTE : sourceCategory;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName == null || groupName.isBlank() ? null : groupName.trim();
    }

    public void setTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.tagsText = null;
            return;
        }
        this.tagsText = tags.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    public void setRetrievalPriority(Integer retrievalPriority) {
        if (retrievalPriority == null) {
            this.retrievalPriority = 3;
            return;
        }
        this.retrievalPriority = Math.max(1, Math.min(5, retrievalPriority));
    }
}
