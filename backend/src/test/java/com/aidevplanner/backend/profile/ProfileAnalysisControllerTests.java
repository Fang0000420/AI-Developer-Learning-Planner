package com.aidevplanner.backend.profile;

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

@WebMvcTest(ProfileAnalysisController.class)
class ProfileAnalysisControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 8, 16, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileAnalysisService profileAnalysisService;

    @Test
    void returnsLatestProfile() throws Exception {
        when(profileAnalysisService.getLatestProfile(10L)).thenReturn(profileResponse());

        mockMvc.perform(get("/api/goals/{goalId}/profile", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(10))
                .andExpect(jsonPath("$.currentSkills[0]").value("Java"))
                .andExpect(jsonPath("$.recommendedDirection").value("Build an MVP AI agent workflow."));

        verify(profileAnalysisService).getLatestProfile(10L);
    }

    @Test
    void returnsNoContentWhenProfileIsMissing() throws Exception {
        when(profileAnalysisService.getLatestProfile(10L)).thenReturn(null);

        mockMvc.perform(get("/api/goals/{goalId}/profile", 10L))
                .andExpect(status().isNoContent());

        verify(profileAnalysisService).getLatestProfile(10L);
    }

    @Test
    void analyzesProfile() throws Exception {
        when(profileAnalysisService.analyzeGoal(10L)).thenReturn(profileResponse());

        mockMvc.perform(post("/api/goals/{goalId}/profile/analyze", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strengths[0]").value("Backend foundation"));

        verify(profileAnalysisService).analyzeGoal(10L);
    }

    @Test
    void returnsBadGatewayWhenAgentFails() throws Exception {
        when(profileAnalysisService.analyzeGoal(10L))
                .thenThrow(new AgentServiceException("Profile analyzer service is unavailable."));

        mockMvc.perform(post("/api/goals/{goalId}/profile/analyze", 10L))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.errors.agentService")
                        .value("Profile analyzer service is unavailable."));
    }

    @Test
    void rejectsNegativeGoalId() throws Exception {
        mockMvc.perform(post("/api/goals/{goalId}/profile/analyze", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(profileAnalysisService);
    }

    private SkillProfileResponse profileResponse() {
        return new SkillProfileResponse(
                20L,
                1L,
                10L,
                List.of("Java"),
                List.of("Backend foundation"),
                List.of("LLM evaluation"),
                "Build an MVP AI agent workflow.",
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
