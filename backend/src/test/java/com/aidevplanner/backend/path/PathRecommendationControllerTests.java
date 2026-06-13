package com.aidevplanner.backend.path;

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

@WebMvcTest(PathRecommendationController.class)
class PathRecommendationControllerTests {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 13, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PathRecommendationService pathRecommendationService;

    @Test
    void returnsLatestPathRecommendation() throws Exception {
        when(pathRecommendationService.getLatestPathRecommendation(10L)).thenReturn(pathResponse());

        mockMvc.perform(get("/api/goals/{goalId}/path-recommendation", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(10))
                .andExpect(jsonPath("$.recommendedPath").value("AI 应用开发成长路径"))
                .andExpect(jsonPath("$.focusAreas[0]").value("关键基础"));

        verify(pathRecommendationService).getLatestPathRecommendation(10L);
    }

    @Test
    void returnsNoContentWhenPathRecommendationIsMissing() throws Exception {
        when(pathRecommendationService.getLatestPathRecommendation(10L)).thenReturn(null);

        mockMvc.perform(get("/api/goals/{goalId}/path-recommendation", 10L))
                .andExpect(status().isNoContent());

        verify(pathRecommendationService).getLatestPathRecommendation(10L);
    }

    @Test
    void analyzesGoalIntoPathRecommendation() throws Exception {
        when(pathRecommendationService.analyzeGoal(10L)).thenReturn(pathResponse());

        mockMvc.perform(post("/api/goals/{goalId}/path-recommendation/analyze", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestones[0]").value("完成基础能力搭建"))
                .andExpect(jsonPath("$.finalDeliverables[0]").value("阶段性项目成果"));

        verify(pathRecommendationService).analyzeGoal(10L);
    }

    @Test
    void returnsBadGatewayWhenPathAnalysisFails() throws Exception {
        when(pathRecommendationService.analyzeGoal(10L))
                .thenThrow(new AgentServiceException("Path recommendation service is unavailable."));

        mockMvc.perform(post("/api/goals/{goalId}/path-recommendation/analyze", 10L))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.errors.agentService")
                        .value("Path recommendation service is unavailable."));
    }

    @Test
    void rejectsNegativeGoalId() throws Exception {
        mockMvc.perform(post("/api/goals/{goalId}/path-recommendation/analyze", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id must be positive."));

        verifyNoInteractions(pathRecommendationService);
    }

    private PathRecommendationResponse pathResponse() {
        return new PathRecommendationResponse(
                90L,
                10L,
                5L,
                70L,
                1,
                "AI 应用开发成长路径",
                "先稳住基础，再通过项目形成可验证成果。",
                "当前已经具备部分开发基础，但需要补齐应用能力。",
                "先围绕“完成基础能力搭建”建立稳定练习。",
                "中等",
                28,
                BigDecimal.valueOf(2),
                List.of("关键基础", "稳定练习", "场景应用"),
                List.of("完成基础能力搭建", "实现第一个小项目"),
                List.of("节奏不稳定会影响进展"),
                List.of("已有基础：Java", "优先补齐：Prompt 设计"),
                List.of("阶段性项目成果", "复盘记录"),
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
