package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.asyncjob.AsyncJobResponse;
import com.aidevplanner.backend.asyncjob.AsyncJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge/documents")
@Validated
public class KnowledgeDocumentController {

    private final AsyncJobService asyncJobService;
    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(
            AsyncJobService asyncJobService,
            KnowledgeDocumentService knowledgeDocumentService
    ) {
        this.asyncJobService = asyncJobService;
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @GetMapping
    public List<KnowledgeDocumentResponse> listDocuments() {
        return knowledgeDocumentService.listDocuments();
    }

    @GetMapping("/retrieval-preview/{goalId}")
    public KnowledgeRetrievalPreviewResponse previewRetrieval(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        return knowledgeDocumentService.previewRetrieval(goalId);
    }

    @GetMapping("/strategy-compare")
    public KnowledgeStrategyComparisonResponse compareStrategies(
            @Positive(message = "Base goal id must be positive.")
            @RequestParam
            Long baseGoalId,
            @Positive(message = "Compare goal id must be positive.")
            @RequestParam
            Long compareGoalId
    ) {
        return knowledgeDocumentService.compareStrategies(baseGoalId, compareGoalId);
    }

    @GetMapping("/{documentId}")
    public KnowledgeDocumentDetailResponse getDocument(
            @Positive(message = "Knowledge document id must be positive.")
            @PathVariable
            Long documentId
    ) {
        return knowledgeDocumentService.getDocument(documentId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KnowledgeUploadResponse> uploadPersonalDocument(
            @RequestParam(required = false)
            String title,
            @RequestParam("file")
            MultipartFile file
    ) {
        KnowledgeDocumentResponse document = knowledgeDocumentService.uploadPersonalDocument(title, file);
        AsyncJobResponse job = asyncJobService.createKnowledgeIngestionJob(
                new KnowledgeIngestionRequest(document.id())
        );
        KnowledgeUploadResponse response = new KnowledgeUploadResponse(document, job.jobId());
        return ResponseEntity
                .created(URI.create("/api/knowledge/documents/" + response.document().id()))
                .body(response);
    }

    @PatchMapping("/{documentId}/enabled")
    public KnowledgeDocumentResponse updateEnabled(
            @Positive(message = "Knowledge document id must be positive.")
            @PathVariable
            Long documentId,
            @Valid @RequestBody KnowledgeDocumentEnabledUpdateRequest request
    ) {
        return knowledgeDocumentService.updateEnabled(documentId, request);
    }

    @PatchMapping("/{documentId}/settings")
    public KnowledgeDocumentResponse updateSettings(
            @Positive(message = "Knowledge document id must be positive.")
            @PathVariable
            Long documentId,
            @Valid @RequestBody KnowledgeDocumentSettingsUpdateRequest request
    ) {
        return knowledgeDocumentService.updateSettings(documentId, request);
    }

    @PatchMapping("/{documentId}/metadata")
    public KnowledgeDocumentResponse updateMetadata(
            @Positive(message = "Knowledge document id must be positive.")
            @PathVariable
            Long documentId,
            @Valid @RequestBody KnowledgeDocumentMetadataUpdateRequest request
    ) {
        return knowledgeDocumentService.updateMetadata(documentId, request);
    }

    @PatchMapping("/batch")
    public List<KnowledgeDocumentResponse> batchUpdate(
            @Valid @RequestBody KnowledgeDocumentBatchUpdateRequest request
    ) {
        return knowledgeDocumentService.batchUpdate(request);
    }
}
