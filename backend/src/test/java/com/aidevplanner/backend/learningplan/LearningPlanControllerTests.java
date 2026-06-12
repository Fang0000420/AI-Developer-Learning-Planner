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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(jsonPath("$.planTitle").value("21-Day Business English speaking track Learning Plan"))
                .andExpect(jsonPath("$.days[0].tasks[0].title").value("Review speaking goals"));

        verify(learningPlanService).generatePlan(10L);
    }

    @Test
    void listsPlans() throws Exception {
        when(learningPlanService.listPlans()).thenReturn(List.of(planSummaryResponse()));

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].goalId").value(10))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
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
    void listsPlanTasks() throws Exception {
        when(learningPlanService.listTasks(30L)).thenReturn(planResponse().days().get(0).tasks());

        mockMvc.perform(get("/api/plans/{planId}/tasks", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(learningPlanService).listTasks(30L);
    }

    @Test
    void returnsTodayTasksForRequestedDayIndex() throws Exception {
        PlanDayResponse day = planResponse().days().get(0);
        when(learningPlanService.getDayTasks(30L, 1)).thenReturn(day);

        mockMvc.perform(get("/api/plans/{planId}/tasks/today", 30L)
                        .param("dayIndex", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayIndex").value(1))
                .andExpect(jsonPath("$.tasks[0].title").value("Review speaking goals"));

        verify(learningPlanService).getDayTasks(30L, 1);
    }

    @Test
    void updatesPlanStatus() throws Exception {
        when(learningPlanService.updatePlanStatus(
                30L,
                new LearningPlanUpdateRequest(LearningPlanStatus.PAUSED)
        )).thenReturn(pausedPlanResponse());

        mockMvc.perform(put("/api/plans/{planId}", 30L)
                        .contentType("application/json")
                        .content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(learningPlanService).updatePlanStatus(
                30L,
                new LearningPlanUpdateRequest(LearningPlanStatus.PAUSED)
        );
    }

    @Test
    void updatesTaskStatus() throws Exception {
        PlanTaskResponse updatedTask = new PlanTaskResponse(
                1L,
                1,
                1,
                "Review speaking goals",
                "Understand the speaking priorities for work scenarios.",
                30,
                "learning",
                "Speaking focus notes",
                "high",
                DailyTaskStatus.IN_PROGRESS
        );
        when(learningPlanService.updateTaskStatus(
                30L,
                1L,
                new DailyTaskStatusUpdateRequest(DailyTaskStatus.IN_PROGRESS)
        )).thenReturn(updatedTask);

        mockMvc.perform(put("/api/plans/{planId}/tasks/{taskId}/status", 30L, 1L)
                        .contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(learningPlanService).updateTaskStatus(
                30L,
                1L,
                new DailyTaskStatusUpdateRequest(DailyTaskStatus.IN_PROGRESS)
        );
    }

    @Test
    void deletesPlan() throws Exception {
        mockMvc.perform(delete("/api/plans/{planId}", 30L))
                .andExpect(status().isNoContent());

        verify(learningPlanService).deletePlan(30L);
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

    @Test
    void rejectsNegativeTaskId() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}/tasks/{taskId}/status", 30L, -1L)
                        .contentType("application/json")
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.taskId").value("Task id must be positive."));

        verifyNoInteractions(learningPlanService);
    }

    private LearningPlanResponse planResponse() {
        return new LearningPlanResponse(
                30L,
                10L,
                1L,
                90L,
                "21-Day Business English speaking track Learning Plan",
                21,
                LearningPlanStatus.ACTIVE,
                List.of(
                        new PlanDayResponse(
                                1,
                                "Speaking foundation",
                                90,
                                List.of(
                                        new PlanTaskResponse(
                                                1L,
                                                1,
                                                1,
                                                "Review speaking goals",
                                                "Understand the speaking priorities for work scenarios.",
                                                30,
                                                "learning",
                                                "Speaking focus notes",
                                                "high",
                                                DailyTaskStatus.PENDING
                                        ),
                                        new PlanTaskResponse(
                                                2L,
                                                1,
                                                2,
                                                "Practice role-play answers",
                                                "Complete one focused speaking practice block.",
                                                60,
                                                "practice",
                                                "Role-play notes",
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

    private LearningPlanResponse pausedPlanResponse() {
        return new LearningPlanResponse(
                30L,
                10L,
                1L,
                90L,
                "21-Day Business English speaking track Learning Plan",
                21,
                LearningPlanStatus.PAUSED,
                List.of(),
                TIMESTAMP,
                TIMESTAMP
        );
    }

    private LearningPlanSummaryResponse planSummaryResponse() {
        return new LearningPlanSummaryResponse(
                30L,
                10L,
                1L,
                "21-Day Business English speaking track Learning Plan",
                21,
                LearningPlanStatus.ACTIVE,
                1,
                2,
                90,
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
