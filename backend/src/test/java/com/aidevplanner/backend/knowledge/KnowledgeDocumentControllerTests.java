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

    private KnowledgeDocumentResponse documentResponse() {
        return new KnowledgeDocumentResponse(
                21L,
                7L,
                "PERSONAL",
                "学习笔记",
                "personal_upload",
                "notes.md",
                "text/markdown",
                1024L,
                "READY",
                true,
                "一份关于模型调用和项目实践的笔记。",
                3L,
                LocalDateTime.of(2026, 6, 13, 12, 0),
                LocalDateTime.of(2026, 6, 13, 11, 30),
                LocalDateTime.of(2026, 6, 13, 12, 0)
        );
    }
}
