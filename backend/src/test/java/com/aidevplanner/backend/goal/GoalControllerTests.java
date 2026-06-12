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
                                  "durationDays": 30,
                                  "responseLanguage": "en",
                                  "dailyAvailableHours": 2.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/goals/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Build an AI Agent project"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.responseLanguage").value("zh"))
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
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].responseLanguage").value("zh"));

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
                                  "durationDays": 30,
                                  "responseLanguage": "en",
                                  "status": "PAUSED",
                                  "dailyAvailableHours": 2.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.responseLanguage").value("zh"))
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
                                  "durationDays": 6,
                                  "dailyAvailableHours": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.title").value("Title is required."))
                .andExpect(jsonPath("$.errors.durationDays").value("Duration days must be between 7 and 60."))
                .andExpect(jsonPath("$.errors.dailyAvailableHours").value("Daily available hours cannot exceed 12."));

        verifyNoInteractions(goalService);
    }

    @Test
    void rejectsUnsupportedDurationDays() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build an AI Agent project",
                                  "description": "Practice production AI Agent workflows.",
                                  "durationDays": 61,
                                  "dailyAvailableHours": 2.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.durationDays").value("Duration days must be between 7 and 60."));

        verifyNoInteractions(goalService);
    }

    @Test
    void rejectsInvalidResponseLanguage() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build an AI Agent project",
                                  "description": "Practice production AI Agent workflows.",
                                  "durationDays": 21,
                                  "responseLanguage": "fr",
                                  "dailyAvailableHours": 2.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.responseLanguage").value("Invalid value for responseLanguage."));

        verifyNoInteractions(goalService);
    }

    @Test
    void rejectsMissingDailyAvailableHours() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build an AI Agent project",
                                  "description": "Practice production AI Agent workflows.",
                                  "durationDays": 21
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.dailyAvailableHours").value("Daily available hours is required."));

        verifyNoInteractions(goalService);
    }

    @Test
    void rejectsNegativeGoalId() throws Exception {
        mockMvc.perform(get("/api/goals/{goalId}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(goalService);
    }

    @Test
    void rejectsNegativeUserIdFilter() throws Exception {
        mockMvc.perform(get("/api/goals").param("userId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.userId").value("User id must be positive."));

        verifyNoInteractions(goalService);
    }

    @Test
    void rejectsInvalidStatusValue() throws Exception {
        mockMvc.perform(get("/api/goals").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.status").value("Invalid value for status."));

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
                30,
                ResponseLanguage.zh,
                status,
                new BigDecimal("2.0"),
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
