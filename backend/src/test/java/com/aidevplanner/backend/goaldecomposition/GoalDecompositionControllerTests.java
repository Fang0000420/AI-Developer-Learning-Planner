package com.aidevplanner.backend.goaldecomposition;

import com.aidevplanner.backend.agent.AgentServiceException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalDecompositionController.class)
class GoalDecompositionControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 8, 18, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalDecompositionService goalDecompositionService;

    @Test
    void returnsLatestDecomposition() throws Exception {
        when(goalDecompositionService.getLatestDecomposition(10L)).thenReturn(decompositionResponse());

        mockMvc.perform(get("/api/goals/{goalId}/decomposition", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(10))
                .andExpect(jsonPath("$.subGoals[0].title").value("Design agent workflow"))
                .andExpect(jsonPath("$.subGoals[0].priority").value("high"));

        verify(goalDecompositionService).getLatestDecomposition(10L);
    }

    @Test
    void returnsNoContentWhenDecompositionIsMissing() throws Exception {
        when(goalDecompositionService.getLatestDecomposition(10L)).thenReturn(null);

        mockMvc.perform(get("/api/goals/{goalId}/decomposition", 10L))
                .andExpect(status().isNoContent());

        verify(goalDecompositionService).getLatestDecomposition(10L);
    }

    @Test
    void decomposesGoal() throws Exception {
        when(goalDecompositionService.decomposeGoal(10L)).thenReturn(decompositionResponse());

        mockMvc.perform(post("/api/goals/{goalId}/decomposition/decompose", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subGoals[1].title").value("Build frontend display"));

        verify(goalDecompositionService).decomposeGoal(10L);
    }

    @Test
    void returnsBadGatewayWhenAgentFails() throws Exception {
        when(goalDecompositionService.decomposeGoal(10L))
                .thenThrow(new AgentServiceException("Goal decomposer service is unavailable."));

        mockMvc.perform(post("/api/goals/{goalId}/decomposition/decompose", 10L))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.errors.agentService")
                        .value("Goal decomposer service is unavailable."));
    }

    @Test
    void rejectsNegativeGoalId() throws Exception {
        mockMvc.perform(post("/api/goals/{goalId}/decomposition/decompose", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(goalDecompositionService);
    }

    private GoalDecompositionResponse decompositionResponse() {
        return new GoalDecompositionResponse(
                30L,
                10L,
                List.of(
                        new SubGoalResponse(
                                "Design agent workflow",
                                "Define nodes and data flow.",
                                "high"
                        ),
                        new SubGoalResponse(
                                "Build frontend display",
                                "Show generated sub-goals.",
                                "medium"
                        )
                ),
                TIMESTAMP
        );
    }
}
