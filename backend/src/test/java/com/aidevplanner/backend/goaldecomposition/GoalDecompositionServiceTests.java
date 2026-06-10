package com.aidevplanner.backend.goaldecomposition;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
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
class GoalDecompositionServiceTests {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private GoalDecomposerClient goalDecomposerClient;

    @Mock
    private GoalRepository goalRepository;

    private GoalDecompositionService goalDecompositionService;

    @BeforeEach
    void setUp() {
        goalDecompositionService = new GoalDecompositionService(
                agentRunRepository,
                authenticatedUserService,
                goalDecomposerClient,
                goalRepository,
                new ObjectMapper()
        );
    }

    @Test
    void decomposesGoalAndPersistsSuccessfulRun() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(goalDecomposerClient.decompose(any(GoalDecomposeRequest.class)))
                .thenReturn(goalDecomposeResponse());
        when(agentRunRepository.save(any(AgentRun.class)))
                .thenAnswer(invocation -> {
                    AgentRun run = invocation.getArgument(0);
                    ReflectionTestUtils.setField(run, "id", 30L);
                    ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 8, 18, 0));
                    return run;
                });

        GoalDecompositionResponse response = goalDecompositionService.decomposeGoal(10L);

        assertThat(response.runId()).isEqualTo(30L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.subGoals()).hasSize(2);
        assertThat(response.subGoals().get(0).title()).isEqualTo("Design agent workflow");

        ArgumentCaptor<GoalDecomposeRequest> requestCaptor =
                ArgumentCaptor.forClass(GoalDecomposeRequest.class);
        verify(goalDecomposerClient).decompose(requestCaptor.capture());
        assertThat(requestCaptor.getValue().mainGoal()).isEqualTo("Build AI agent apps");
        assertThat(requestCaptor.getValue().background())
                .isEqualTo("Backend developer with Java and PostgreSQL experience.");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(runCaptor.getValue().getAgentName()).isEqualTo("Goal Decomposer");
        assertThat(runCaptor.getValue().getInputJson()).contains("Build AI agent apps");
        assertThat(runCaptor.getValue().getOutputJson()).contains("subGoals");
    }

    @Test
    void savesFailedRunWhenAgentFails() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(goalDecomposerClient.decompose(any(GoalDecomposeRequest.class)))
                .thenThrow(new AgentServiceException("Goal decomposer service is unavailable."));

        assertThatThrownBy(() -> goalDecompositionService.decomposeGoal(10L))
                .isInstanceOf(AgentServiceException.class)
                .hasMessage("Goal decomposer service is unavailable.");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage())
                .isEqualTo("Goal decomposer service is unavailable.");
    }

    @Test
    void returnsLatestSuccessfulDecomposition() {
        AgentRun run = agentRun();
        when(goalRepository.existsById(10L)).thenReturn(true);
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Goal Decomposer",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(run));

        GoalDecompositionResponse response = goalDecompositionService.getLatestDecomposition(10L);

        assertThat(response.runId()).isEqualTo(40L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.subGoals()).extracting(SubGoalResponse::priority)
                .containsExactly("high", "medium");
    }

    private AgentRun agentRun() {
        Goal goal = goal();
        AgentRun run = new AgentRun(
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
                            },
                            {
                              "title": "Build frontend display",
                              "description": "Show generated sub-goals.",
                              "priority": "medium"
                            }
                          ]
                        }
                        """,
                AgentRunStatus.SUCCESS,
                12L,
                null
        );
        ReflectionTestUtils.setField(run, "id", 40L);
        ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 8, 18, 30));
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

    private GoalDecomposeResponse goalDecomposeResponse() {
        return new GoalDecomposeResponse(List.of(
                new SubGoalResponse(
                        "Design agent workflow",
                        "Define nodes and data flow.",
                        "high"
                ),
                new SubGoalResponse(
                        "Build frontend display",
                        "Show generated sub-goals.",
                        "medium"
                )
        ));
    }
}
