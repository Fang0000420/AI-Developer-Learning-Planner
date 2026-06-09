package com.aidevplanner.backend.progress;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressLogController.class)
class ProgressLogControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 9, 14, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressLogService progressLogService;

    @Test
    void submitsProgress() throws Exception {
        ProgressSubmitRequest request = new ProgressSubmitRequest(
                30L,
                1,
                "Completed the backend slice.",
                List.of(1L),
                List.of(2L),
                List.of("Need frontend verification")
        );
        when(progressLogService.submitProgress(request)).thenReturn(progressLogResponse());

        mockMvc.perform(post("/api/progress")
                        .contentType("application/json")
                        .content("""
                                {
                                  "planId": 30,
                                  "dayIndex": 1,
                                  "userFeedback": "Completed the backend slice.",
                                  "completedTaskIds": [1],
                                  "unfinishedTaskIds": [2],
                                  "blockers": ["Need frontend verification"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.completedTaskIds[0]").value(1))
                .andExpect(jsonPath("$.unfinishedTaskIds[0]").value(2))
                .andExpect(jsonPath("$.blockers[0]").value("Need frontend verification"));

        verify(progressLogService).submitProgress(request);
    }

    @Test
    void listsProgress() throws Exception {
        when(progressLogService.listProgress(30L, 1)).thenReturn(List.of(progressLogResponse()));

        mockMvc.perform(get("/api/progress/{planId}", 30L)
                        .param("dayIndex", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].dayIndex").value(1))
                .andExpect(jsonPath("$[0].userFeedback").value("Completed the backend slice."));

        verify(progressLogService).listProgress(30L, 1);
    }

    @Test
    void rejectsMissingFeedback() throws Exception {
        mockMvc.perform(post("/api/progress")
                        .contentType("application/json")
                        .content("""
                                {
                                  "planId": 30,
                                  "dayIndex": 1,
                                  "completedTaskIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userFeedback").value("User feedback is required."));

        verifyNoInteractions(progressLogService);
    }

    @Test
    void rejectsNegativePlanId() throws Exception {
        mockMvc.perform(get("/api/progress/{planId}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.planId").value("Plan id must be positive."));

        verifyNoInteractions(progressLogService);
    }

    private ProgressLogResponse progressLogResponse() {
        return new ProgressLogResponse(
                100L,
                30L,
                10L,
                1L,
                1,
                "Completed the backend slice.",
                List.of(1L),
                List.of(2L),
                List.of("Need frontend verification"),
                Map.of(),
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
