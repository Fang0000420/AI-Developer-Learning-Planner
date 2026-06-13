package com.aidevplanner.backend.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void returnsCurrentUserProfile() throws Exception {
        when(userProfileService.getCurrentUserProfile()).thenReturn(response());

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value(3))
                .andExpect(jsonPath("$.preferredLearningStyle").value("项目驱动"))
                .andExpect(jsonPath("$.recentSnapshots[0].goalId").value(10));
    }

    @Test
    void returnsNoContentWhenCurrentUserProfileIsMissing() throws Exception {
        when(userProfileService.getCurrentUserProfile()).thenReturn(null);

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatesCurrentUserProfile() throws Exception {
        when(userProfileService.updateCurrentUserProfile(new UserProfileUpdateRequest(
                "项目驱动",
                "稳步推进",
                "工作日 1.5 小时，周末 3 小时。",
                "我更适合先做小项目。"
        ))).thenReturn(response());

        mockMvc.perform(patch("/api/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "preferredLearningStyle":"项目驱动",
                                  "pacePreference":"稳步推进",
                                  "timeBudgetNote":"工作日 1.5 小时，周末 3 小时。",
                                  "manualCorrection":"我更适合先做小项目。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualCorrection").value("我更适合先做小项目。"));

        verify(userProfileService).updateCurrentUserProfile(new UserProfileUpdateRequest(
                "项目驱动",
                "稳步推进",
                "工作日 1.5 小时，周末 3 小时。",
                "我更适合先做小项目。"
        ));
    }

    private UserProfileResponse response() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 13, 14, 0);
        return new UserProfileResponse(
                1L,
                7L,
                3,
                "当前长期画像显示，适合围绕项目驱动路线持续输出。",
                "项目驱动",
                "稳步推进",
                "工作日 1.5 小时，周末 3 小时。",
                "我更适合先做小项目。",
                List.of("Java", "Spring Boot"),
                List.of("后端基础"),
                List.of("LLM 评估"),
                List.of("项目练习", "Prompt 设计"),
                List.of("节奏不稳定"),
                List.of("已有基础：Java"),
                "围绕 AI 应用路线持续建立成果。",
                List.of(new GoalProfileSnapshotResponse(
                        12L,
                        10L,
                        "Build AI agent apps",
                        4L,
                        3,
                        "围绕目标形成的一次画像快照。",
                        "项目驱动",
                        "稳步推进",
                        "工作日 1.5 小时，周末 3 小时。",
                        List.of("Java"),
                        List.of("后端基础"),
                        List.of("LLM 评估"),
                        List.of("项目练习"),
                        List.of("节奏不稳定"),
                        List.of("已有基础：Java"),
                        "围绕 AI 应用路线持续建立成果。",
                        timestamp,
                        timestamp
                )),
                timestamp,
                timestamp
        );
    }
}
