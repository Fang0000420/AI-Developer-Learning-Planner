package com.aidevplanner.backend.learningplan;

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
import com.aidevplanner.backend.knowledge.KnowledgeContextBundle;
import com.aidevplanner.backend.knowledge.KnowledgeContextService;
import com.aidevplanner.backend.path.PathRecommendationRepository;
import com.aidevplanner.backend.profile.SkillProfile;
import com.aidevplanner.backend.profile.SkillProfileRepository;
import com.aidevplanner.backend.profile.UserProfile;
import com.aidevplanner.backend.profile.UserProfileRepository;
import com.aidevplanner.backend.progress.ProgressLogRepository;
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
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private DailyTaskRepository dailyTaskRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private LearningPlanVersionManager learningPlanVersionManager;

    @Mock
    private KnowledgeContextService knowledgeContextService;

    @Mock
    private PathRecommendationRepository pathRecommendationRepository;

    @Mock
    private PlanGeneratorClient planGeneratorClient;

    @Mock
    private ProgressLogRepository progressLogRepository;

    @Mock
    private SkillProfileRepository skillProfileRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private LearningPlanService learningPlanService;

    @BeforeEach
    void setUp() {
        learningPlanService = new LearningPlanService(
                agentRunRepository,
                authenticatedUserService,
                dailyTaskRepository,
                goalRepository,
                knowledgeContextService,
                learningPlanRepository,
                learningPlanVersionManager,
                new ObjectMapper().findAndRegisterModules(),
                planGeneratorClient,
                pathRecommendationRepository,
                progressLogRepository,
                skillProfileRepository,
                userProfileRepository
        );
    }

    @Test
    void generatesPlanAndPersistsPlanAndTasks() {
        Goal goal = goal();
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(knowledgeContextService.buildForGoal(goal)).thenReturn(new KnowledgeContextBundle(
                "Source: Work notes\nSummary: Real customer call notes\nExcerpt: Needs short speaking drills for work.",
                List.of("Knowledge evidence from \"Work notes\": Needs short speaking drills for work."),
                List.of("Work notes"),
                1
        ));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(userProfile(goal)));
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(skillProfile(goal)));
        when(learningPlanRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
        when(pathRecommendationRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
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
                .thenReturn(AgentClientResponse.model(planResponse()));
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
        when(learningPlanVersionManager.versions(any(LearningPlan.class))).thenReturn(List.of());

        LearningPlanResponse response = learningPlanService.generatePlan(10L);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.sourceAgentRunId()).isEqualTo(90L);
        assertThat(response.planTitle()).isEqualTo("30-Day Business English speaking track Learning Plan");
        assertThat(response.days()).hasSize(30);
        assertThat(response.days().get(0).tasks()).hasSize(2);

        ArgumentCaptor<PlanGenerateAgentRequest> requestCaptor =
                ArgumentCaptor.forClass(PlanGenerateAgentRequest.class);
        verify(planGeneratorClient).generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().mainGoal()).isEqualTo("Improve business English speaking");
        assertThat(requestCaptor.getValue().currentSkills()).containsExactly("Reading comprehension", "Basic vocabulary");
        assertThat(requestCaptor.getValue().recommendedProject()).isEqualTo("Business English speaking track");
        assertThat(requestCaptor.getValue().userProfileSummary()).contains("项目驱动");
        assertThat(requestCaptor.getValue().knowledgeContext()).contains("Work notes");
        assertThat(requestCaptor.getValue().planningConstraints()).isNotEmpty();
        assertThat(requestCaptor.getValue().subGoals()).extracting(SubGoalResponse::title)
                .containsExactly("Build a daily speaking routine");
        assertThat(requestCaptor.getValue().skillGaps()).extracting(SkillGapResponse::skill)
                .containsExactly("Speaking fluency");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getAgentName()).isEqualTo("Plan Generator");
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(runCaptor.getValue().getResponseSource()).isEqualTo(AgentResponseSource.MODEL);
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
        when(knowledgeContextService.buildForGoal(goal)).thenReturn(KnowledgeContextBundle.empty());
        when(skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.empty());
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
        when(pathRecommendationRepository.findFirstByGoalIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
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
        when(learningPlanVersionManager.versions(plan)).thenReturn(List.of(
                new LearningPlanVersionSummaryResponse(
                        2,
                        "progress_adjustment",
                        "Adjusted after feedback.",
                        2,
                        3,
                        180,
                        15,
                        0,
                        List.of(2),
                        new LearningPlanVersionDiffResponse(
                                1,
                                0,
                                1,
                                List.of(2),
                                List.of(
                                        new LearningPlanVersionTaskChangeResponse(
                                                2,
                                                "Record a speaking sample",
                                                "updated",
                                                75,
                                                90
                                        )
                                )
                        ),
                        true,
                        LocalDateTime.of(2026, 6, 10, 9, 0)
                )
        ));

        LearningPlanResponse response = learningPlanService.getPlan(30L);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.days()).hasSize(2);
        assertThat(response.days().get(0).totalEstimatedMinutes()).isEqualTo(90);
        assertThat(response.days().get(1).tasks()).hasSize(1);
        assertThat(response.versions()).hasSize(1);
    }

    @Test
    void listsTasksForPlan() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.existsById(30L)).thenReturn(true);
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L))
                .thenReturn(tasks(plan, goal));

        List<PlanTaskResponse> response = learningPlanService.listTasks(30L);

        assertThat(response).hasSize(3);
        assertThat(response.get(0).title()).isEqualTo("Review speaking goals");
        assertThat(response.get(0).status()).isEqualTo(DailyTaskStatus.PENDING);
    }

    @Test
    void returnsTasksForRequestedDay() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.existsById(30L)).thenReturn(true);
        when(dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(30L, 2))
                .thenReturn(List.of(tasks(plan, goal).get(2)));

        PlanDayResponse response = learningPlanService.getDayTasks(30L, 2);

        assertThat(response.dayIndex()).isEqualTo(2);
        assertThat(response.theme()).isEqualTo("Applied speaking");
        assertThat(response.tasks()).hasSize(1);
    }

    @Test
    void updatesTaskStatusWhenTaskBelongsToPlan() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        DailyTask task = tasks(plan, goal).get(0);
        when(learningPlanRepository.existsById(30L)).thenReturn(true);
        when(dailyTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        PlanTaskResponse response = learningPlanService.updateTaskStatus(
                30L,
                1L,
                new DailyTaskStatusUpdateRequest(DailyTaskStatus.DONE)
        );

        assertThat(response.status()).isEqualTo(DailyTaskStatus.DONE);
        assertThat(task.getStatus()).isEqualTo(DailyTaskStatus.DONE);
    }

    @Test
    void rejectsTaskStatusUpdateForDifferentPlan() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        DailyTask task = tasks(plan, goal).get(0);
        when(learningPlanRepository.existsById(99L)).thenReturn(true);
        when(dailyTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> learningPlanService.updateTaskStatus(
                99L,
                1L,
                new DailyTaskStatusUpdateRequest(DailyTaskStatus.DONE)
        ))
                .isInstanceOf(com.aidevplanner.backend.common.ResourceNotFoundException.class)
                .hasMessage("Daily task in learning plan 1 was not found.");
    }

    @Test
    void listsPlanSummaries() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(plan));
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L))
                .thenReturn(tasks(plan, goal));

        List<LearningPlanSummaryResponse> response = learningPlanService.listPlans();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(30L);
        assertThat(response.get(0).goalId()).isEqualTo(10L);
        assertThat(response.get(0).status()).isEqualTo(LearningPlanStatus.ACTIVE);
        assertThat(response.get(0).dayCount()).isEqualTo(2);
        assertThat(response.get(0).taskCount()).isEqualTo(3);
        assertThat(response.get(0).totalEstimatedMinutes()).isEqualTo(180);
    }

    @Test
    void updatesPlanStatus() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L))
                .thenReturn(tasks(plan, goal));
        when(learningPlanVersionManager.versions(plan)).thenReturn(List.of());

        LearningPlanResponse response =
                learningPlanService.updatePlanStatus(
                        30L,
                        new LearningPlanUpdateRequest(LearningPlanStatus.PAUSED)
                );

        assertThat(response.status()).isEqualTo(LearningPlanStatus.PAUSED);
        assertThat(plan.getStatus()).isEqualTo(LearningPlanStatus.PAUSED);
    }

    @Test
    void deletesPlan() {
        when(learningPlanRepository.existsById(30L)).thenReturn(true);

        learningPlanService.deletePlan(30L);

        verify(learningPlanRepository).deleteById(30L);
    }

    @Test
    void restoresPlanVersion() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L))
                .thenReturn(tasks(plan, goal));
        when(learningPlanVersionManager.versions(plan)).thenReturn(List.of(
                new LearningPlanVersionSummaryResponse(
                        3,
                        "restore",
                        "Restored from version 1.",
                        2,
                        3,
                        180,
                        0,
                        0,
                        List.of(1, 2),
                        new LearningPlanVersionDiffResponse(
                                0,
                                0,
                                1,
                                List.of(1, 2),
                                List.of(
                                        new LearningPlanVersionTaskChangeResponse(
                                                1,
                                                "Review speaking goals",
                                                "updated",
                                                45,
                                                30
                                        )
                                )
                        ),
                        true,
                        LocalDateTime.of(2026, 6, 10, 10, 0)
                )
        ));

        LearningPlanResponse response = learningPlanService.restoreVersion(30L, 1);

        verify(learningPlanVersionManager).restoreVersion(plan, 1);
        assertThat(response.versions()).hasSize(1);
        assertThat(response.versions().get(0).trigger()).isEqualTo("restore");
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
                              "title": "Build a daily speaking routine",
                              "description": "Practice speaking every day with feedback.",
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
                              "skill": "Speaking fluency",
                              "currentLevel": "beginner",
                              "targetLevel": "intermediate",
                              "priority": "high",
                              "reason": "Needed to communicate more naturally at work."
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
    }

    private PlanGenerateAgentResponse planResponse() {
        return new PlanGenerateAgentResponse(
                "30-Day Business English speaking track Learning Plan",
                30,
                List.of(
                        new PlanDayAgentResponse(
                                1,
                                "Speaking foundation",
                                List.of(
                                        new PlanTaskAgentResponse(
                                                "Review speaking goals",
                                                "Understand the speaking priorities for work scenarios.",
                                                30,
                                                "learning",
                                                "Speaking focus notes",
                                                "high"
                                        ),
                                        new PlanTaskAgentResponse(
                                                "Practice role-play answers",
                                                "Complete one focused speaking practice block.",
                                                60,
                                                "practice",
                                                "Role-play notes",
                                                "high"
                                        )
                                )
                        ),
                        new PlanDayAgentResponse(
                                2,
                                "Applied speaking",
                                List.of(
                                        new PlanTaskAgentResponse(
                                                "Record a speaking sample",
                                                "Record one short simulated work conversation.",
                                                90,
                                                "practice",
                                                "Speaking recording",
                                                "high"
                                        ),
                                        new PlanTaskAgentResponse(
                                                "Review the recording",
                                                "Note pronunciation and phrasing issues.",
                                                30,
                                                "review",
                                                "Review notes",
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
                "30-Day Business English speaking track Learning Plan",
                21,
                java.util.Map.of("planTitle", "30-Day Business English speaking track Learning Plan")
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
                "Speaking foundation",
                "Review speaking goals",
                "Understand the speaking priorities for work scenarios.",
                30,
                "learning",
                "Speaking focus notes",
                "high"
        );
        DailyTask second = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                1,
                2,
                "Speaking foundation",
                "Practice role-play answers",
                "Complete one focused speaking practice block.",
                60,
                "practice",
                "Role-play notes",
                "high"
        );
        DailyTask third = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                2,
                1,
                "Applied speaking",
                "Record a speaking sample",
                "Record one short simulated work conversation.",
                90,
                "practice",
                "Speaking recording",
                "high"
        );
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        ReflectionTestUtils.setField(third, "id", 3L);
        return List.of(first, second, third);
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

    private UserProfile userProfile(Goal goal) {
        UserProfile profile = new UserProfile(
                goal.getUser(),
                2,
                "长期画像显示该用户更适合项目驱动、短反馈周期的学习方式。",
                "项目驱动",
                "稳步推进",
                "工作日 2 小时。",
                "我更适合短时高频练习。",
                List.of("Reading comprehension", "Basic vocabulary"),
                List.of("Regular learning habit"),
                List.of("Speaking fluency"),
                List.of("Speaking drills", "Role-play"),
                List.of("如果任务过长，容易中断。"),
                List.of("知识库证据：真实工作场景需要短 speaking drills。"),
                "围绕工作场景持续建立 speaking 输出。"
        );
        ReflectionTestUtils.setField(profile, "id", 88L);
        return profile;
    }
}
