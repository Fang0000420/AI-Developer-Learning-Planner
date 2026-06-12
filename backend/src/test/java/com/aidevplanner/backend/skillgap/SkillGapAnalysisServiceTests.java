package com.aidevplanner.backend.skillgap;

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
class SkillGapAnalysisServiceTests {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SkillGapAnalyzerClient skillGapAnalyzerClient;

    @Mock
    private SkillProfileRepository skillProfileRepository;

    private SkillGapAnalysisService skillGapAnalysisService;

    @BeforeEach
    void setUp() {
        skillGapAnalysisService = new SkillGapAnalysisService(
                agentRunRepository,
                authenticatedUserService,
                goalRepository,
                new ObjectMapper(),
                skillGapAnalyzerClient,
                skillProfileRepository
        );
    }

    @Test
    void analyzesSkillGapAndPersistsSuccessfulRun() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(skillProfile(goal)));
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Goal Decomposer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(goalDecompositionRun(goal)));
        when(skillGapAnalyzerClient.analyze(any(SkillGapAnalyzeRequest.class)))
                .thenReturn(AgentClientResponse.model(skillGapAnalyzeResponse()));
        when(agentRunRepository.save(any(AgentRun.class)))
                .thenAnswer(invocation -> {
                    AgentRun run = invocation.getArgument(0);
                    ReflectionTestUtils.setField(run, "id", 50L);
                    ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 9, 10, 0));
                    return run;
                });

        SkillGapAnalysisResponse response = skillGapAnalysisService.analyzeSkillGap(10L);

        assertThat(response.runId()).isEqualTo(50L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.skillGaps()).hasSize(4);
        assertThat(response.skillGaps().get(0).priority()).isEqualTo("high");

        ArgumentCaptor<SkillGapAnalyzeRequest> requestCaptor =
                ArgumentCaptor.forClass(SkillGapAnalyzeRequest.class);
        verify(skillGapAnalyzerClient).analyze(requestCaptor.capture());
        assertThat(requestCaptor.getValue().mainGoal()).isEqualTo("Build AI agent apps");
        assertThat(requestCaptor.getValue().currentSkills()).containsExactly("Java", "Spring Boot");
        assertThat(requestCaptor.getValue().weaknesses()).containsExactly("LLM evaluation");
        assertThat(requestCaptor.getValue().subGoals()).extracting(SubGoalResponse::title)
                .containsExactly("Design agent workflow");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(runCaptor.getValue().getResponseSource()).isEqualTo(AgentResponseSource.MODEL);
        assertThat(runCaptor.getValue().getAgentName()).isEqualTo("Skill Gap Analyzer");
        assertThat(runCaptor.getValue().getInputJson()).contains("currentSkills");
        assertThat(runCaptor.getValue().getOutputJson()).contains("skillGaps");
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
        when(skillGapAnalyzerClient.analyze(any(SkillGapAnalyzeRequest.class)))
                .thenThrow(new AgentServiceException("Skill gap analyzer service is unavailable."));

        assertThatThrownBy(() -> skillGapAnalysisService.analyzeSkillGap(10L))
                .isInstanceOf(AgentServiceException.class)
                .hasMessage("Skill gap analyzer service is unavailable.");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage())
                .isEqualTo("Skill gap analyzer service is unavailable.");
    }

    @Test
    void returnsLatestSuccessfulSkillGapAnalysis() {
        AgentRun run = skillGapRun(goal());
        when(goalRepository.existsById(10L)).thenReturn(true);
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Skill Gap Analyzer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(run));

        SkillGapAnalysisResponse response = skillGapAnalysisService.getLatestSkillGapAnalysis(10L);

        assertThat(response.runId()).isEqualTo(60L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.skillGaps()).extracting(SkillGapResponse::skill)
                .containsExactly(
                        "AI agent workflow design",
                        "Structured LLM output validation",
                        "Full-stack planning integration",
                        "Evaluation loop"
                );
    }

    private AgentRun goalDecompositionRun(Goal goal) {
        return new AgentRun(
                goal.getUser(),
                goal,
                "Goal Decomposer",
                "{\"mainGoal\":\"Build AI agent apps\"}",
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
        AgentRun run = new AgentRun(
                goal.getUser(),
                goal,
                "Skill Gap Analyzer",
                "{\"mainGoal\":\"Build AI agent apps\"}",
                """
                        {
                          "skillGaps": [
                            {
                              "skill": "AI agent workflow design",
                              "currentLevel": "basic",
                              "targetLevel": "production-ready",
                              "priority": "high",
                              "reason": "Needed for reliable planner behavior."
                            },
                            {
                              "skill": "Structured LLM output validation",
                              "currentLevel": "beginner",
                              "targetLevel": "intermediate",
                              "priority": "high",
                              "reason": "Needed for schema-safe agent responses."
                            },
                            {
                              "skill": "Full-stack planning integration",
                              "currentLevel": "basic",
                              "targetLevel": "intermediate",
                              "priority": "medium",
                              "reason": "Needed to connect agent output to the UI."
                            },
                            {
                              "skill": "Evaluation loop",
                              "currentLevel": "beginner",
                              "targetLevel": "intermediate",
                              "priority": "medium",
                              "reason": "Needed to improve daily plans."
                            }
                          ]
                        }
                        """,
                AgentRunStatus.SUCCESS,
                15L,
                null
        );
        ReflectionTestUtils.setField(run, "id", 60L);
        ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 9, 11, 0));
        return run;
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

    private SkillProfile skillProfile(Goal goal) {
        return new SkillProfile(
                goal.getUser(),
                goal,
                List.of("Java", "Spring Boot"),
                List.of("Backend foundation"),
                List.of("LLM evaluation"),
                "Build an MVP AI agent workflow."
        );
    }

    private SkillGapAnalyzeResponse skillGapAnalyzeResponse() {
        return new SkillGapAnalyzeResponse(List.of(
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
        ));
    }
}
