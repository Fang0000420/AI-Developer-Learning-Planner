package com.aidevplanner.backend.goal;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
class GoalControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 8, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @Test
    void createsGoal() throws Exception {
        GoalResponse response = goalResponse(10L, GoalStatus.ACTIVE);
        when(goalService.createGoal(any(GoalCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/goals")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build an AI Agent project",
                                  "description": "Practice production AI Agent workflows.",
                                  "durationDays": 21,
                                  "dailyAvailableHours": 2.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/goals/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Build an AI Agent project"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dailyAvailableHours").value(2.0));

        verify(goalService).createGoal(any(GoalCreateRequest.class));
    }

    @Test
    void listsGoals() throws Exception {
        when(goalService.listGoals(1L, GoalStatus.ACTIVE))
                .thenReturn(List.of(goalResponse(10L, GoalStatus.ACTIVE)));

        mockMvc.perform(get("/api/goals")
                        .param("userId", "1")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(goalService).listGoals(1L, GoalStatus.ACTIVE);
    }

    @Test
    void returnsGoalDetails() throws Exception {
        when(goalService.getGoal(10L)).thenReturn(goalResponse(10L, GoalStatus.ACTIVE));

        mockMvc.perform(get("/api/goals/{goalId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.description").value("Practice production AI Agent workflows."));

        verify(goalService).getGoal(10L);
    }

    @Test
    void updatesGoal() throws Exception {
        GoalResponse response = goalResponse(10L, GoalStatus.PAUSED);
        when(goalService.updateGoal(any(Long.class), any(GoalUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/goals/{goalId}", 10L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build an AI Agent project",
                                  "description": "Practice production AI Agent workflows.",
                                  "durationDays": 21,
                                  "status": "PAUSED",
                                  "dailyAvailableHours": 2.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(goalService).updateGoal(any(Long.class), any(GoalUpdateRequest.class));
    }

    @Test
    void deletesGoal() throws Exception {
        mockMvc.perform(delete("/api/goals/{goalId}", 10L))
                .andExpect(status().isNoContent());

        verify(goalService).deleteGoal(10L);
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "durationDays": 0,
                                  "dailyAvailableHours": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.title").value("Title is required."))
                .andExpect(jsonPath("$.errors.durationDays").value("Duration days must be at least 1."))
                .andExpect(jsonPath("$.errors.dailyAvailableHours").value("Daily available hours cannot exceed 12."));

        verifyNoInteractions(goalService);
    }

    @Test
    void returnsNotFoundForMissingGoal() throws Exception {
        when(goalService.getGoal(404L)).thenThrow(new ResourceNotFoundException("Goal", 404L));

        mockMvc.perform(get("/api/goals/{goalId}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Goal 404 was not found."));
    }

    private GoalResponse goalResponse(Long goalId, GoalStatus status) {
        return new GoalResponse(
                goalId,
                1L,
                "Build an AI Agent project",
                "Practice production AI Agent workflows.",
                21,
                status,
                new BigDecimal("2.0"),
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
