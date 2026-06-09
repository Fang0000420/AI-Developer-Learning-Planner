package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
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
class LearningPlanServiceTests {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private DailyTaskRepository dailyTaskRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private PlanGeneratorClient planGeneratorClient;

    @Mock
    private SkillProfileRepository skillProfileRepository;

    private LearningPlanService learningPlanService;

    @BeforeEach
    void setUp() {
        learningPlanService = new LearningPlanService(
                agentRunRepository,
                dailyTaskRepository,
                goalRepository,
                learningPlanRepository,
                new ObjectMapper(),
                planGeneratorClient,
                skillProfileRepository
        );
    }

    @Test
    void generatesPlanAndPersistsPlanAndTasks() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(skillProfile(goal)));
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Project Recommender",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.of(projectRun(goal)));
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
        when(planGeneratorClient.generate(any(PlanGenerateAgentRequest.class)))
                .thenReturn(planResponse());
        when(agentRunRepository.save(any(AgentRun.class)))
                .thenAnswer(invocation -> {
                    AgentRun run = invocation.getArgument(0);
                    ReflectionTestUtils.setField(run, "id", 90L);
                    ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 6, 9, 13, 0));
                    return run;
                });
        when(learningPlanRepository.save(any(LearningPlan.class)))
                .thenAnswer(invocation -> {
                    LearningPlan plan = invocation.getArgument(0);
                    ReflectionTestUtils.setField(plan, "id", 30L);
                    ReflectionTestUtils.setField(plan, "createdAt", LocalDateTime.of(2026, 6, 9, 13, 1));
                    ReflectionTestUtils.setField(plan, "updatedAt", LocalDateTime.of(2026, 6, 9, 13, 1));
                    return plan;
                });
        when(dailyTaskRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<DailyTask> tasks = invocation.getArgument(0);
                    for (int index = 0; index < tasks.size(); index++) {
                        ReflectionTestUtils.setField(tasks.get(index), "id", (long) index + 1);
                    }
                    return tasks;
                });

        LearningPlanResponse response = learningPlanService.generatePlan(10L);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.sourceAgentRunId()).isEqualTo(90L);
        assertThat(response.planTitle()).isEqualTo("21-Day AI Planner MVP Plan");
        assertThat(response.days()).hasSize(21);
        assertThat(response.days().get(0).tasks()).hasSize(2);

        ArgumentCaptor<PlanGenerateAgentRequest> requestCaptor =
                ArgumentCaptor.forClass(PlanGenerateAgentRequest.class);
        verify(planGeneratorClient).generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().mainGoal()).isEqualTo("Build AI agent apps");
        assertThat(requestCaptor.getValue().currentSkills()).containsExactly("Java", "Spring Boot");
        assertThat(requestCaptor.getValue().recommendedProject()).isEqualTo("AI Developer Learning Planner");
        assertThat(requestCaptor.getValue().subGoals()).extracting(SubGoalResponse::title)
                .containsExactly("Design agent workflow");
        assertThat(requestCaptor.getValue().skillGaps()).extracting(SkillGapResponse::skill)
                .containsExactly("Structured LLM output validation");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getAgentName()).isEqualTo("Plan Generator");
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(runCaptor.getValue().getOutputJson()).contains("planTitle");

        ArgumentCaptor<LearningPlan> planCaptor = ArgumentCaptor.forClass(LearningPlan.class);
        verify(learningPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getPlanJson()).containsKey("days");

        ArgumentCaptor<List<DailyTask>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(dailyTaskRepository).saveAll(taskCaptor.capture());
        assertThat(taskCaptor.getValue()).hasSizeGreaterThan(4);
        assertThat(taskCaptor.getValue().get(0).getDayIndex()).isEqualTo(1);
        assertThat(taskCaptor.getValue().get(0).getTaskOrder()).isEqualTo(1);
    }

    @Test
    void savesFailedRunWhenAgentFails() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.empty());
        when(agentRunRepository.findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                10L,
                "Project Recommender",
                AgentRunStatus.SUCCESS
        )).thenReturn(Optional.empty());
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
        when(planGeneratorClient.generate(any(PlanGenerateAgentRequest.class)))
                .thenThrow(new AgentServiceException("Plan generator service is unavailable."));

        assertThatThrownBy(() -> learningPlanService.generatePlan(10L))
                .isInstanceOf(AgentServiceException.class)
                .hasMessage("Plan generator service is unavailable.");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage())
                .isEqualTo("Plan generator service is unavailable.");
    }

    @Test
    void returnsPlanDetailWithGroupedTasks() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L))
                .thenReturn(tasks(plan, goal));

        LearningPlanResponse response = learningPlanService.getPlan(30L);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.days()).hasSize(2);
        assertThat(response.days().get(0).totalEstimatedMinutes()).isEqualTo(90);
        assertThat(response.days().get(1).tasks()).hasSize(1);
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
        return new AgentRun(
                goal.getUser(),
                goal,
                "Skill Gap Analyzer",
                "{\"mainGoal\":\"Build AI agent apps\"}",
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
        return new AgentRun(
                goal.getUser(),
                goal,
                "Project Recommender",
                "{\"mainGoal\":\"Build AI agent apps\"}",
                """
                        {
                          "recommendedProject": "AI Developer Learning Planner",
                          "reason": "Covers full-stack agent workflow practice.",
                          "difficulty": "medium-high",
                          "durationDays": 21,
                          "dailyTimeHours": 2,
                          "coreTechStack": ["Spring Boot", "FastAPI", "Next.js"],
                          "finalDeliverables": ["Complete GitHub repository", "Runnable full-stack demo"]
                        }
                        """,
                AgentRunStatus.SUCCESS,
                18L,
                null
        );
    }

    private PlanGenerateAgentResponse planResponse() {
        return new PlanGenerateAgentResponse(
                "21-Day AI Planner MVP Plan",
                21,
                List.of(
                        new PlanDayAgentResponse(
                                1,
                                "Foundation setup",
                                List.of(
                                        new PlanTaskAgentResponse(
                                                "Review architecture",
                                                "Understand service boundaries.",
                                                30,
                                                "learning",
                                                "Architecture notes",
                                                "high"
                                        ),
                                        new PlanTaskAgentResponse(
                                                "Create plan endpoint",
                                                "Implement the backend endpoint.",
                                                60,
                                                "build",
                                                "Working endpoint",
                                                "high"
                                        )
                                )
                        ),
                        new PlanDayAgentResponse(
                                2,
                                "Agent integration",
                                List.of(
                                        new PlanTaskAgentResponse(
                                                "Call plan generator",
                                                "Connect backend to FastAPI.",
                                                90,
                                                "build",
                                                "Agent client integration",
                                                "high"
                                        ),
                                        new PlanTaskAgentResponse(
                                                "Verify persistence",
                                                "Save plan and tasks.",
                                                30,
                                                "test",
                                                "Persistence check",
                                                "medium"
                                        )
                                )
                        )
                )
        );
    }

    private LearningPlan learningPlan(Goal goal) {
        LearningPlan plan = new LearningPlan(
                goal.getUser(),
                goal,
                null,
                "21-Day AI Planner MVP Plan",
                21,
                java.util.Map.of("planTitle", "21-Day AI Planner MVP Plan")
        );
        ReflectionTestUtils.setField(plan, "id", 30L);
        ReflectionTestUtils.setField(plan, "createdAt", LocalDateTime.of(2026, 6, 9, 13, 1));
        ReflectionTestUtils.setField(plan, "updatedAt", LocalDateTime.of(2026, 6, 9, 13, 1));
        return plan;
    }

    private List<DailyTask> tasks(LearningPlan plan, Goal goal) {
        DailyTask first = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                1,
                1,
                "Foundation setup",
                "Review architecture",
                "Understand service boundaries.",
                30,
                "learning",
                "Architecture notes",
                "high"
        );
        DailyTask second = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                1,
                2,
                "Foundation setup",
                "Create plan endpoint",
                "Implement the backend endpoint.",
                60,
                "build",
                "Working endpoint",
                "high"
        );
        DailyTask third = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                2,
                1,
                "Agent integration",
                "Call plan generator",
                "Connect backend to FastAPI.",
                90,
                "build",
                "Agent client integration",
                "high"
        );
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        ReflectionTestUtils.setField(third, "id", 3L);
        return List.of(first, second, third);
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
}
