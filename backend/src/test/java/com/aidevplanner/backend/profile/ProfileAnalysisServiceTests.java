package com.aidevplanner.backend.profile;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentResponseSource;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.knowledge.KnowledgeContextBundle;
import com.aidevplanner.backend.knowledge.KnowledgeContextService;
import com.aidevplanner.backend.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAnalysisServiceTests {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ProfileAnalyzerClient profileAnalyzerClient;

    @Mock
    private SkillProfileRepository skillProfileRepository;

    @Mock
    private KnowledgeContextService knowledgeContextService;

    @Mock
    private UserProfileService userProfileService;

    private ProfileAnalysisService profileAnalysisService;

    @BeforeEach
    void setUp() {
        profileAnalysisService = new ProfileAnalysisService(
                agentRunRepository,
                authenticatedUserService,
                goalRepository,
                new ObjectMapper().findAndRegisterModules(),
                profileAnalyzerClient,
                skillProfileRepository,
                knowledgeContextService,
                userProfileService
        );
    }

    @Test
    void analyzesGoalAndPersistsProfileAndRun() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(knowledgeContextService.buildForGoal(goal)).thenReturn(new KnowledgeContextBundle(
                "Source: Resume\nExcerpt: Spring Boot delivery experience",
                List.of("知识库证据《Resume》：Spring Boot delivery experience"),
                List.of("Resume"),
                1
        ));
        when(profileAnalyzerClient.analyze(any(ProfileAnalyzeRequest.class)))
                .thenReturn(AgentClientResponse.model(profileAnalyzeResponse()));
        when(skillProfileRepository.save(any(SkillProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkillProfileResponse response = profileAnalysisService.analyzeGoal(10L);

        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.currentSkills()).containsExactly("Java", "Spring Boot");
        assertThat(response.recommendedDirection()).isEqualTo("Build an MVP AI agent workflow.");

        ArgumentCaptor<ProfileAnalyzeRequest> requestCaptor =
                ArgumentCaptor.forClass(ProfileAnalyzeRequest.class);
        verify(profileAnalyzerClient).analyze(requestCaptor.capture());
        assertThat(requestCaptor.getValue().background())
                .isEqualTo("Backend developer with Java and PostgreSQL experience.");
        assertThat(requestCaptor.getValue().goal()).isEqualTo("Build AI agent apps");
        assertThat(requestCaptor.getValue().dailyAvailableHours()).isEqualByComparingTo("2.0");
        assertThat(requestCaptor.getValue().knowledgeContext()).contains("Resume");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(runCaptor.getValue().getResponseSource()).isEqualTo(AgentResponseSource.MODEL);
        assertThat(runCaptor.getValue().getInputJson()).contains("Build AI agent apps");
        assertThat(runCaptor.getValue().getOutputJson()).contains("recommendedDirection");
        verify(userProfileService).recordGoalAnalysis(any(Goal.class), any(SkillProfile.class), any(ProfileAnalyzeResponse.class));
    }

    @Test
    void savesFailedRunWhenAgentFails() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(knowledgeContextService.buildForGoal(goal)).thenReturn(KnowledgeContextBundle.empty());
        when(profileAnalyzerClient.analyze(any(ProfileAnalyzeRequest.class)))
                .thenThrow(new AgentServiceException("Profile analyzer service is unavailable."));

        assertThatThrownBy(() -> profileAnalysisService.analyzeGoal(10L))
                .isInstanceOf(AgentServiceException.class)
                .hasMessage("Profile analyzer service is unavailable.");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage())
                .isEqualTo("Profile analyzer service is unavailable.");
    }

    private Goal goal() {
        User user = new User("demo-user", "demo@example.com", "not-used");
        user.setBackground("Backend developer with Java and PostgreSQL experience.");
        user.setDailyAvailableHours(new BigDecimal("2.0"));
        ReflectionTestUtils.setField(user, "id", 1L);

        Goal goal = new Goal(user, "Build AI agent apps", 21);
        goal.setDescription("Learn production AI agent workflows.");
        ReflectionTestUtils.setField(goal, "id", 10L);
        return goal;
    }

    private ProfileAnalyzeResponse profileAnalyzeResponse() {
        return new ProfileAnalyzeResponse(
                List.of("Java", "Spring Boot"),
                List.of("Backend foundation"),
                List.of("LLM evaluation"),
                "Build an MVP AI agent workflow."
        );
    }
}
