package com.aidevplanner.backend.projectrecommendation;

import com.aidevplanner.backend.agent.AgentServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectRecommendationController.class)
class ProjectRecommendationControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 9, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectRecommendationService projectRecommendationService;

    @Test
    void returnsLatestProjectRecommendation() throws Exception {
        when(projectRecommendationService.getLatestProjectRecommendation(10L)).thenReturn(projectResponse());

        mockMvc.perform(get("/api/goals/{goalId}/project-recommendation", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(10))
                .andExpect(jsonPath("$.recommendedProject").value("Business English speaking track"))
                .andExpect(jsonPath("$.coreTechStack[0]").value("Listening input"));

        verify(projectRecommendationService).getLatestProjectRecommendation(10L);
    }

    @Test
    void returnsNoContentWhenProjectRecommendationIsMissing() throws Exception {
        when(projectRecommendationService.getLatestProjectRecommendation(10L)).thenReturn(null);

        mockMvc.perform(get("/api/goals/{goalId}/project-recommendation", 10L))
                .andExpect(status().isNoContent());

        verify(projectRecommendationService).getLatestProjectRecommendation(10L);
    }

    @Test
    void recommendsProject() throws Exception {
        when(projectRecommendationService.recommendProject(10L)).thenReturn(projectResponse());

        mockMvc.perform(post("/api/goals/{goalId}/project-recommendation/recommend", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalDeliverables[0]").value("Speaking recordings"));

        verify(projectRecommendationService).recommendProject(10L);
    }

    @Test
    void returnsBadGatewayWhenAgentFails() throws Exception {
        when(projectRecommendationService.recommendProject(10L))
                .thenThrow(new AgentServiceException("Project recommender service is unavailable."));

        mockMvc.perform(post("/api/goals/{goalId}/project-recommendation/recommend", 10L))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.errors.agentService")
                        .value("Project recommender service is unavailable."));
    }

    @Test
    void rejectsNegativeGoalId() throws Exception {
        mockMvc.perform(post("/api/goals/{goalId}/project-recommendation/recommend", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(projectRecommendationService);
    }

    private ProjectRecommendationResponse projectResponse() {
        return new ProjectRecommendationResponse(
                70L,
                10L,
                "Business English speaking track",
                "Uses a focused speaking track with repeated practice.",
                "medium",
                21,
                BigDecimal.valueOf(2),
                List.of("Listening input", "Role-play", "Speaking feedback"),
                List.of("Speaking recordings", "Phrase bank"),
                TIMESTAMP
        );
    }
}
