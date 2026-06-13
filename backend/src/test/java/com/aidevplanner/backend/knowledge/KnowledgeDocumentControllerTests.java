package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.asyncjob.AsyncJobResponse;
import com.aidevplanner.backend.asyncjob.AsyncJobService;
import com.aidevplanner.backend.asyncjob.AsyncJobStatus;
import com.aidevplanner.backend.asyncjob.AsyncJobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeDocumentController.class)
class KnowledgeDocumentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsyncJobService asyncJobService;

    @MockitoBean
    private KnowledgeDocumentService knowledgeDocumentService;

    @Test
    void listsKnowledgeDocuments() throws Exception {
        when(knowledgeDocumentService.listDocuments()).thenReturn(List.of(documentResponse()));

        mockMvc.perform(get("/api/knowledge/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("学习笔记"))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].chunkCount").value(3));
    }

    @Test
    void previewsKnowledgeRetrieval() throws Exception {
        when(knowledgeDocumentService.previewRetrieval(31L)).thenReturn(
                new KnowledgeRetrievalPreviewResponse(
                        31L,
                        "Build AI agent apps",
                        4,
                        2,
                        2,
                        List.of("个人资料优先。"),
                        List.of(
                                new KnowledgeRetrievalPreviewMatchResponse(
                                        21L,
                                        "学习笔记",
                                        "PERSONAL",
                                        "NOTE",
                                        3,
                                        "求职资料",
                                        List.of("agent"),
                                        18,
                                        true,
                                        List.of("标题命中目标关键词"),
                                        List.of("Built AI agent apps with Spring Boot.")
                                )
                        )
                )
        );

        mockMvc.perform(get("/api/knowledge/documents/retrieval-preview/{goalId}", 31L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(31))
                .andExpect(jsonPath("$.matches[0].title").value("学习笔记"))
                .andExpect(jsonPath("$.matches[0].selectedForContext").value(true));
    }

    @Test
    void comparesKnowledgeStrategiesBetweenGoals() throws Exception {
        when(knowledgeDocumentService.compareStrategies(31L, 32L)).thenReturn(
                new KnowledgeStrategyComparisonResponse(
                        31L,
                        "Build AI agent apps",
                        32L,
                        "Improve business English",
                        new com.aidevplanner.backend.goal.GoalKnowledgePreferenceResponse(31L, List.of(21L), "PERSONAL", List.of("PROJECT")),
                        new com.aidevplanner.backend.goal.GoalKnowledgePreferenceResponse(32L, List.of(22L), "PLATFORM", List.of("COURSE")),
                        List.of("两个目标固定的作用域不同。"),
                        List.of(
                                new KnowledgeStrategyComparisonDocumentResponse(
                                        21L,
                                        "学习笔记",
                                        "PERSONAL",
                                        "NOTE",
                                        18,
                                        null,
                                        null,
                                        true,
                                        false,
                                        List.of("agent")
                                )
                        ),
                        List.of(),
                        List.of()
                )
        );

        mockMvc.perform(get("/api/knowledge/documents/strategy-compare")
                        .param("baseGoalId", "31")
                        .param("compareGoalId", "32"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseGoalId").value(31))
                .andExpect(jsonPath("$.compareGoalId").value(32))
                .andExpect(jsonPath("$.onlyInBase[0].title").value("学习笔记"));
    }

    @Test
    void uploadsPersonalDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "text/markdown",
                "# hello".getBytes()
        );
        UUID jobId = UUID.randomUUID();
        when(knowledgeDocumentService.uploadPersonalDocument(eq("学习笔记"), any()))
                .thenReturn(documentResponse());
        when(asyncJobService.createKnowledgeIngestionJob(new KnowledgeIngestionRequest(21L)))
                .thenReturn(new AsyncJobResponse(
                        jobId,
                        AsyncJobType.KNOWLEDGE_INGESTION,
                        AsyncJobStatus.PENDING,
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 13, 12, 0),
                        LocalDateTime.of(2026, 6, 13, 12, 0)
                ));

        mockMvc.perform(multipart("/api/knowledge/documents")
                        .file(file)
                        .param("title", "学习笔记"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/knowledge/documents/21"))
                .andExpect(jsonPath("$.document.id").value(21))
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));

        verify(knowledgeDocumentService).uploadPersonalDocument(eq("学习笔记"), any());
    }

    @Test
    void updatesEnabledStatus() throws Exception {
        when(knowledgeDocumentService.updateEnabled(eq(21L), any()))
                .thenReturn(documentResponse());

        mockMvc.perform(patch("/api/knowledge/documents/{documentId}/enabled", 21L)
                        .contentType("application/json")
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void updatesKnowledgeSettings() throws Exception {
        when(knowledgeDocumentService.updateSettings(eq(21L), any()))
                .thenReturn(documentResponse());

        mockMvc.perform(patch("/api/knowledge/documents/{documentId}/settings", 21L)
                        .contentType("application/json")
                        .content("{\"scope\":\"PERSONAL\",\"retrievalPriority\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("PERSONAL"))
                .andExpect(jsonPath("$.retrievalPriority").value(3));
    }

    @Test
    void updatesKnowledgeMetadata() throws Exception {
        when(knowledgeDocumentService.updateMetadata(eq(21L), any()))
                .thenReturn(documentResponse());

        mockMvc.perform(patch("/api/knowledge/documents/{documentId}/metadata", 21L)
                        .contentType("application/json")
                        .content("""
                                {
                                  "scope":"PERSONAL",
                                  "retrievalPriority":4,
                                  "sourceCategory":"PROJECT",
                                  "groupName":"作品集",
                                  "tags":["agent","planner"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCategory").value("NOTE"))
                .andExpect(jsonPath("$.groupName").value("求职资料"))
                .andExpect(jsonPath("$.tags[0]").value("agent"));
    }

    @Test
    void batchUpdatesKnowledgeDocuments() throws Exception {
        when(knowledgeDocumentService.batchUpdate(any()))
                .thenReturn(List.of(documentResponse()));

        mockMvc.perform(patch("/api/knowledge/documents/batch")
                        .contentType("application/json")
                        .content("""
                                {
                                  "documentIds":[21,22],
                                  "enabled":true,
                                  "sourceCategory":"PROJECT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].sourceCategory").value("NOTE"));
    }

    private KnowledgeDocumentResponse documentResponse() {
        return new KnowledgeDocumentResponse(
                21L,
                7L,
                "PERSONAL",
                "NOTE",
                "求职资料",
                List.of("agent", "planner"),
                "学习笔记",
                "personal_upload",
                "notes.md",
                "text/markdown",
                1024L,
                "READY",
                true,
                3,
                "一份关于模型调用和项目实践的笔记。",
                3L,
                LocalDateTime.of(2026, 6, 13, 12, 0),
                LocalDateTime.of(2026, 6, 13, 11, 30),
                LocalDateTime.of(2026, 6, 13, 12, 0)
        );
    }
}
