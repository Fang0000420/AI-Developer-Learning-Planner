package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.user.User;
import com.aidevplanner.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class KnowledgeDocumentService {

    private static final int CHUNK_SIZE = 1000;
    private static final int PREVIEW_SUMMARY_LENGTH = 220;

    private final AuthenticatedUserService authenticatedUserService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final Path knowledgeStorageRoot;
    private final UserRepository userRepository;

    public KnowledgeDocumentService(
            AuthenticatedUserService authenticatedUserService,
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            @Value("${app.storage.knowledge-root:${user.dir}/storage/knowledge}") String knowledgeStorageRoot,
            UserRepository userRepository
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeStorageRoot = Path.of(knowledgeStorageRoot);
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listDocuments() {
        User user = currentUser();
        return knowledgeDocumentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(document -> KnowledgeDocumentResponse.from(
                        document,
                        knowledgeChunkRepository.countByDocumentId(document.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentDetailResponse getDocument(Long documentId) {
        KnowledgeDocument document = findOwnedDocument(documentId);
        return KnowledgeDocumentDetailResponse.from(
                document,
                knowledgeChunkRepository.countByDocumentId(documentId)
        );
    }

    public KnowledgeDocumentResponse uploadPersonalDocument(String title, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document file is required.");
        }
        User user = currentUser();
        String originalFileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "knowledge-document.txt"
                : file.getOriginalFilename().trim();
        Path userDirectory = knowledgeStorageRoot.resolve(String.valueOf(user.getId()));
        try {
            Files.createDirectories(userDirectory);
            String safeFileName = sanitizeFileName(originalFileName);
            Path target = userDirectory.resolve(UUID.randomUUID() + "-" + safeFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            KnowledgeDocument savedDocument = knowledgeDocumentRepository.save(
                    new KnowledgeDocument(
                            user,
                            KnowledgeDocumentScope.PERSONAL,
                            (title == null || title.isBlank()) ? stripExtension(originalFileName) : title.trim(),
                            "personal_upload",
                            originalFileName,
                            file.getContentType(),
                            file.getSize(),
                            target.toString()
                    )
            );
            return KnowledgeDocumentResponse.from(savedDocument, 0);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store knowledge document.", exception);
        }
    }

    @Transactional
    public KnowledgeDocumentResponse updateEnabled(Long documentId, KnowledgeDocumentEnabledUpdateRequest request) {
        KnowledgeDocument document = findOwnedDocument(documentId);
        document.setEnabled(request.enabled());
        KnowledgeDocument saved = knowledgeDocumentRepository.save(document);
        return KnowledgeDocumentResponse.from(saved, knowledgeChunkRepository.countByDocumentId(saved.getId()));
    }

    @Transactional
    public KnowledgeDocumentResponse ingestDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document", documentId));
        document.markProcessing();
        knowledgeDocumentRepository.save(document);

        try {
            String rawText = extractText(document);
            if (rawText.isBlank()) {
                throw new IllegalStateException("The uploaded file does not contain readable text.");
            }
            knowledgeChunkRepository.deleteByDocumentId(documentId);
            List<KnowledgeChunk> chunks = buildChunks(document, rawText);
            knowledgeChunkRepository.saveAll(chunks);
            document.markReady(rawText, summarize(rawText));
            KnowledgeDocument saved = knowledgeDocumentRepository.save(document);
            return KnowledgeDocumentResponse.from(saved, chunks.size());
        } catch (Exception exception) {
            document.markFailed(exception.getMessage() == null
                    ? "Knowledge ingestion failed."
                    : exception.getMessage());
            KnowledgeDocument saved = knowledgeDocumentRepository.save(document);
            throw new IllegalStateException(
                    saved.getSummary() == null ? "Knowledge ingestion failed." : saved.getSummary(),
                    exception
            );
        }
    }

    private KnowledgeDocument findOwnedDocument(Long documentId) {
        User user = currentUser();
        return knowledgeDocumentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document", documentId));
    }

    private User currentUser() {
        Long currentUserId = authenticatedUserService.currentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User", "current"));
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
    }

    private String extractText(KnowledgeDocument document) throws IOException {
        String fileName = document.getOriginalFileName().toLowerCase(Locale.ROOT);
        String mimeType = document.getMimeType() == null ? "" : document.getMimeType().toLowerCase(Locale.ROOT);
        boolean textLike = mimeType.startsWith("text/")
                || mimeType.contains("json")
                || mimeType.contains("xml")
                || fileName.endsWith(".md")
                || fileName.endsWith(".txt")
                || fileName.endsWith(".csv")
                || fileName.endsWith(".json")
                || fileName.endsWith(".xml");
        if (!textLike) {
            throw new IllegalStateException("Only text, markdown, csv, json, and xml files are supported in this phase.");
        }

        String rawText = Files.readString(Path.of(document.getStoragePath()), StandardCharsets.UTF_8);
        return rawText.replace("\r\n", "\n").trim();
    }

    private List<KnowledgeChunk> buildChunks(KnowledgeDocument document, String rawText) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String normalized = rawText.replace("\r", "").trim();
        int index = 0;
        int chunkIndex = 1;
        while (index < normalized.length()) {
            int end = Math.min(normalized.length(), index + CHUNK_SIZE);
            if (end < normalized.length()) {
                int paragraphBreak = normalized.lastIndexOf("\n\n", end);
                if (paragraphBreak > index + 200) {
                    end = paragraphBreak;
                }
            }
            String content = normalized.substring(index, end).trim();
            if (!content.isBlank()) {
                chunks.add(new KnowledgeChunk(document, chunkIndex++, content, content.length()));
            }
            index = Math.max(end, index + 1);
            while (index < normalized.length() && Character.isWhitespace(normalized.charAt(index))) {
                index++;
            }
        }
        if (chunks.isEmpty()) {
            chunks.add(new KnowledgeChunk(document, 1, normalized, normalized.length()));
        }
        return chunks;
    }

    private String summarize(String rawText) {
        String normalized = rawText.replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= PREVIEW_SUMMARY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_SUMMARY_LENGTH) + "...";
    }

    private String sanitizeFileName(String originalFileName) {
        return originalFileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String stripExtension(String value) {
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex <= 0) {
            return value;
        }
        return value.substring(0, dotIndex);
    }
}
