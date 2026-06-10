package com.aidevplanner.backend.agent;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentRunController.class)
class AgentRunControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 10, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentRunQueryService agentRunQueryService;

    @Test
    void listsAgentRunsWithFilters() throws Exception {
        when(agentRunQueryService.listRuns(10L, 20L, "Plan Generator"))
                .thenReturn(List.of(summaryResponse()));

        mockMvc.perform(get("/api/agent-runs")
                        .param("goalId", "10")
                        .param("planId", "20")
                        .param("agentName", "Plan Generator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].goalId").value(10))
                .andExpect(jsonPath("$[0].planId").value(20))
                .andExpect(jsonPath("$[0].agentName").value("Plan Generator"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].requestId").value("req-123"));

        verify(agentRunQueryService).listRuns(10L, 20L, "Plan Generator");
    }

    @Test
    void returnsAgentRunDetail() throws Exception {
        when(agentRunQueryService.getRun(100L)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/agent-runs/{runId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.inputJson.goalTitle").value("Build planner"))
                .andExpect(jsonPath("$.outputJson.planTitle").value("Planner build plan"));

        verify(agentRunQueryService).getRun(100L);
    }

    @Test
    void rejectsNegativeFilters() throws Exception {
        mockMvc.perform(get("/api/agent-runs").param("goalId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(agentRunQueryService);
    }

    @Test
    void returnsNotFoundForMissingRun() throws Exception {
        when(agentRunQueryService.getRun(404L)).thenThrow(new ResourceNotFoundException("Agent run", 404L));

        mockMvc.perform(get("/api/agent-runs/{runId}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    private AgentRunSummaryResponse summaryResponse() {
        return new AgentRunSummaryResponse(
                100L,
                1L,
                10L,
                20L,
                "Plan Generator",
                AgentRunStatus.SUCCESS,
                250L,
                null,
                "req-123",
                TIMESTAMP
        );
    }

    private AgentRunDetailResponse detailResponse() {
        return new AgentRunDetailResponse(
                100L,
                1L,
                10L,
                20L,
                "Plan Generator",
                AgentRunStatus.SUCCESS,
                250L,
                null,
                "req-123",
                objectMapper.createObjectNode().put("goalTitle", "Build planner"),
                objectMapper.createObjectNode().put("planTitle", "Planner build plan"),
                TIMESTAMP
        );
    }
}
