package com.aidevplanner.backend.learningplan;

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

@WebMvcTest(LearningPlanController.class)
class LearningPlanControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 9, 13, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LearningPlanService learningPlanService;

    @Test
    void generatesPlan() throws Exception {
        when(learningPlanService.generatePlan(10L)).thenReturn(planResponse());

        mockMvc.perform(post("/api/plans/generate")
                        .contentType("application/json")
                        .content("{\"goalId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.goalId").value(10))
                .andExpect(jsonPath("$.planTitle").value("21-Day AI Planner MVP Plan"))
                .andExpect(jsonPath("$.days[0].tasks[0].title").value("Review architecture"));

        verify(learningPlanService).generatePlan(10L);
    }

    @Test
    void listsPlans() throws Exception {
        when(learningPlanService.listPlans()).thenReturn(List.of(planSummaryResponse()));

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].goalId").value(10))
                .andExpect(jsonPath("$[0].taskCount").value(2));

        verify(learningPlanService).listPlans();
    }

    @Test
    void returnsPlanDetail() throws Exception {
        when(learningPlanService.getPlan(30L)).thenReturn(planResponse());

        mockMvc.perform(get("/api/plans/{planId}", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.days[0].totalEstimatedMinutes").value(90));

        verify(learningPlanService).getPlan(30L);
    }

    @Test
    void returnsBadGatewayWhenAgentFails() throws Exception {
        when(learningPlanService.generatePlan(10L))
                .thenThrow(new AgentServiceException("Plan generator service is unavailable."));

        mockMvc.perform(post("/api/plans/generate")
                        .contentType("application/json")
                        .content("{\"goalId\":10}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.errors.agentService")
                        .value("Plan generator service is unavailable."));
    }

    @Test
    void rejectsMissingGoalId() throws Exception {
        mockMvc.perform(post("/api/plans/generate")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id is required."));

        verifyNoInteractions(learningPlanService);
    }

    @Test
    void rejectsNegativePlanId() throws Exception {
        mockMvc.perform(get("/api/plans/{planId}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.planId").value("Plan id must be positive."));

        verifyNoInteractions(learningPlanService);
    }

    private LearningPlanResponse planResponse() {
        return new LearningPlanResponse(
                30L,
                10L,
                1L,
                90L,
                "21-Day AI Planner MVP Plan",
                21,
                List.of(
                        new PlanDayResponse(
                                1,
                                "Foundation setup",
                                90,
                                List.of(
                                        new PlanTaskResponse(
                                                1L,
                                                1,
                                                1,
                                                "Review architecture",
                                                "Understand service boundaries.",
                                                30,
                                                "learning",
                                                "Architecture notes",
                                                "high",
                                                DailyTaskStatus.PENDING
                                        ),
                                        new PlanTaskResponse(
                                                2L,
                                                1,
                                                2,
                                                "Create plan endpoint",
                                                "Implement the backend endpoint.",
                                                60,
                                                "build",
                                                "Working endpoint",
                                                "high",
                                                DailyTaskStatus.PENDING
                                        )
                                )
                        )
                ),
                TIMESTAMP,
                TIMESTAMP
        );
    }

    private LearningPlanSummaryResponse planSummaryResponse() {
        return new LearningPlanSummaryResponse(
                30L,
                10L,
                1L,
                "21-Day AI Planner MVP Plan",
                21,
                1,
                2,
                90,
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
