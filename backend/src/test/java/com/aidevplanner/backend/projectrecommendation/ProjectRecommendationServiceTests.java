package com.aidevplanner.backend.projectrecommendation;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentResponseSource;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;
import com.aidevplanner.backend.profile.SkillProfile;
import com.aidevplanner.backend.profile.SkillProfileRepository;
import com.aidevplanner.backend.skillgap.SkillGapResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectRecommendationServiceTests {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ProjectRecommenderClient projectRecommenderClient;

    @Mock
    private SkillProfileRepository skillProfileRepository;

    private ProjectRecommendationService projectRecommendationService;

    @BeforeEach
    void setUp() {
        projectRecommendationService = new ProjectRecommendationService(
                agentRunRepository,
                authenticatedUserService,
                goalRepository,
                new ObjectMapper(),
                projectRecommenderClient,
                skillProfileRepository
        );
    }

    @Test
    void recommendsProjectAndPersistsSuccessfulRun() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(skillProfile(goal)));
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Goal Decomposer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(goalDecompositionRun(goal)));
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Skill Gap Analyzer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(skillGapRun(goal)));
        when(projectRecommenderClient.recommend(any(ProjectRecommendRequest.class)))
                .thenReturn(AgentClientResponse.model(projectRecommendResponse()));
        when(agentRunRepository.save(any(AgentRun.class)))
                .thenAnswer(invocation -> {
                    AgentRun run = invocation.getArgument(0);
                    ReflectionTestUtils.setField(run, "id", 70L);
                    ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 9, 12, 0));
                    return run;
                });

        ProjectRecommendationResponse response = projectRecommendationService.recommendProject(10L);

        assertThat(response.runId()).isEqualTo(70L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.recommendedProject()).isEqualTo("Business English speaking track");
        assertThat(response.durationDays()).isEqualTo(30);
        assertThat(response.dailyTimeHours()).isEqualByComparingTo("2.0");

        ArgumentCaptor<ProjectRecommendRequest> requestCaptor =
                ArgumentCaptor.forClass(ProjectRecommendRequest.class);
        verify(projectRecommenderClient).recommend(requestCaptor.capture());
        assertThat(requestCaptor.getValue().mainGoal()).isEqualTo("Improve business English speaking");
        assertThat(requestCaptor.getValue().currentSkills()).containsExactly("Reading comprehension", "Basic vocabulary");
        assertThat(requestCaptor.getValue().subGoals()).extracting(SubGoalResponse::title)
                .containsExactly("Design agent workflow");
        assertThat(requestCaptor.getValue().skillGaps()).extracting(SkillGapResponse::skill)
                .containsExactly("Structured LLM output validation");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(runCaptor.getValue().getResponseSource()).isEqualTo(AgentResponseSource.MODEL);
        assertThat(runCaptor.getValue().getAgentName()).isEqualTo("Project Recommender");
        assertThat(runCaptor.getValue().getInputJson()).contains("skillGaps");
        assertThat(runCaptor.getValue().getOutputJson()).contains("recommendedProject");
    }

    @Test
    void savesFailedRunWhenAgentFails() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.empty());
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Goal Decomposer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.empty());
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Skill Gap Analyzer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.empty());
        when(projectRecommenderClient.recommend(any(ProjectRecommendRequest.class)))
                .thenThrow(new AgentServiceException("Project recommender service is unavailable."));

        assertThatThrownBy(() -> projectRecommendationService.recommendProject(10L))
                .isInstanceOf(AgentServiceException.class)
                .hasMessage("Project recommender service is unavailable.");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage())
                .isEqualTo("Project recommender service is unavailable.");
    }

    @Test
    void returnsLatestSuccessfulProjectRecommendation() {
        AgentRun run = projectRun(goal());
        when(goalRepository.existsById(10L)).thenReturn(true);
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Project Recommender",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(run));

        ProjectRecommendationResponse response =
                projectRecommendationService.getLatestProjectRecommendation(10L);

        assertThat(response.runId()).isEqualTo(80L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.recommendedProject()).isEqualTo("Business English speaking track");
        assertThat(response.coreTechStack()).containsExactly("Listening input", "Role-play", "Speaking feedback");
    }

    private AgentRun goalDecompositionRun(Goal goal) {
        return new AgentRun(
                goal.getUser(),
                goal,
                "Goal Decomposer",
                "{\"mainGoal\":\"Improve business English speaking\"}",
                """
                        {
                          "subGoals": [
                            {
                              "title": "Design agent workflow",
                              "description": "Define nodes and data flow.",
                              "priority": "high"
                            }
                          ]
                        }
                        """,
                AgentRunStatus.SUCCESS,
                12L,
                null
        );
    }

    private AgentRun skillGapRun(Goal goal) {
        return new AgentRun(
                goal.getUser(),
                goal,
                "Skill Gap Analyzer",
                "{\"mainGoal\":\"Improve business English speaking\"}",
                """
                        {
                          "skillGaps": [
                            {
                              "skill": "Structured LLM output validation",
                              "currentLevel": "beginner",
                              "targetLevel": "intermediate",
                              "priority": "high",
                              "reason": "Needed for schema-safe agent responses."
                            }
                          ]
                        }
                        """,
                AgentRunStatus.SUCCESS,
                15L,
                null
        );
    }

    private AgentRun projectRun(Goal goal) {
        AgentRun run = new AgentRun(
                goal.getUser(),
                goal,
                "Project Recommender",
                "{\"mainGoal\":\"Improve business English speaking\"}",
                """
                        {
                          "recommendedProject": "Business English speaking track",
                          "reason": "Uses a focused speaking track with repeated practice.",
                          "difficulty": "medium",
                          "durationDays": 30,
                          "dailyTimeHours": 2,
                          "coreTechStack": ["Listening input", "Role-play", "Speaking feedback"],
                          "finalDeliverables": ["Speaking recordings", "Phrase bank"]
                        }
                        """,
                AgentRunStatus.SUCCESS,
                18L,
                null
        );
        ReflectionTestUtils.setField(run, "id", 80L);
        ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 9, 12, 30));
        return run;
    }

    private Goal goal() {
        User user = new User("demo-user", "demo@example.com", "not-used");
        user.setBackground("Customer support specialist with strong reading habits.");
        user.setDailyAvailableHours(new BigDecimal("2.0"));
        ReflectionTestUtils.setField(user, "id", 1L);

        Goal goal = new Goal(user, "Improve business English speaking", 30);
        goal.setDescription("Build stronger spoken communication for work scenarios.");
        ReflectionTestUtils.setField(goal, "id", 10L);
        return goal;
    }

    private SkillProfile skillProfile(Goal goal) {
        return new SkillProfile(
                goal.getUser(),
                goal,
                List.of("Reading comprehension", "Basic vocabulary"),
                List.of("Regular learning habit"),
                List.of("Speaking fluency"),
                "Build a focused speaking routine with weekly review."
        );
    }

    private ProjectRecommendResponse projectRecommendResponse() {
        return new ProjectRecommendResponse(
                "Business English speaking track",
                "Uses a focused speaking track with repeated practice.",
                "medium",
                30,
                BigDecimal.valueOf(2),
                List.of("Listening input", "Role-play", "Speaking feedback"),
                List.of("Speaking recordings", "Phrase bank")
        );
    }
}
