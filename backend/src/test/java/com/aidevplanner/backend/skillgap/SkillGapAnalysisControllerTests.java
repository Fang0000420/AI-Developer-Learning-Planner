package com.aidevplanner.backend.skillgap;

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

@WebMvcTest(SkillGapAnalysisController.class)
class SkillGapAnalysisControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 9, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillGapAnalysisService skillGapAnalysisService;

    @Test
    void returnsLatestSkillGapAnalysis() throws Exception {
        when(skillGapAnalysisService.getLatestSkillGapAnalysis(10L)).thenReturn(skillGapResponse());

        mockMvc.perform(get("/api/goals/{goalId}/skill-gap", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(10))
                .andExpect(jsonPath("$.skillGaps[0].skill").value("AI agent workflow design"))
                .andExpect(jsonPath("$.skillGaps[0].priority").value("high"));

        verify(skillGapAnalysisService).getLatestSkillGapAnalysis(10L);
    }

    @Test
    void returnsNoContentWhenSkillGapAnalysisIsMissing() throws Exception {
        when(skillGapAnalysisService.getLatestSkillGapAnalysis(10L)).thenReturn(null);

        mockMvc.perform(get("/api/goals/{goalId}/skill-gap", 10L))
                .andExpect(status().isNoContent());

        verify(skillGapAnalysisService).getLatestSkillGapAnalysis(10L);
    }

    @Test
    void analyzesSkillGap() throws Exception {
        when(skillGapAnalysisService.analyzeSkillGap(10L)).thenReturn(skillGapResponse());

        mockMvc.perform(post("/api/goals/{goalId}/skill-gap/analyze", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillGaps[1].skill").value("Structured LLM output validation"));

        verify(skillGapAnalysisService).analyzeSkillGap(10L);
    }

    @Test
    void returnsBadGatewayWhenAgentFails() throws Exception {
        when(skillGapAnalysisService.analyzeSkillGap(10L))
                .thenThrow(new AgentServiceException("Skill gap analyzer service is unavailable."));

        mockMvc.perform(post("/api/goals/{goalId}/skill-gap/analyze", 10L))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.errors.agentService")
                        .value("Skill gap analyzer service is unavailable."));
    }

    @Test
    void rejectsNegativeGoalId() throws Exception {
        mockMvc.perform(post("/api/goals/{goalId}/skill-gap/analyze", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(skillGapAnalysisService);
    }

    private SkillGapAnalysisResponse skillGapResponse() {
        return new SkillGapAnalysisResponse(
                50L,
                10L,
                List.of(
                        new SkillGapResponse(
                                "AI agent workflow design",
                                "basic",
                                "production-ready",
                                "high",
                                "Needed for reliable planner behavior."
                        ),
                        new SkillGapResponse(
                                "Structured LLM output validation",
                                "beginner",
                                "intermediate",
                                "high",
                                "Needed for schema-safe agent responses."
                        ),
                        new SkillGapResponse(
                                "Full-stack planning integration",
                                "basic",
                                "intermediate",
                                "medium",
                                "Needed to connect agent output to the UI."
                        ),
                        new SkillGapResponse(
                                "Evaluation loop",
                                "beginner",
                                "intermediate",
                                "medium",
                                "Needed to improve daily plans."
                        )
                ),
                TIMESTAMP
        );
    }
}
