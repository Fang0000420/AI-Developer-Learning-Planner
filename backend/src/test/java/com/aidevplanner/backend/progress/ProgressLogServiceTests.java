package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentResponseSource;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.learningplan.DailyTask;
import com.aidevplanner.backend.learningplan.DailyTaskRepository;
import com.aidevplanner.backend.learningplan.DailyTaskStatus;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.learningplan.LearningPlanRepository;
import com.aidevplanner.backend.learningplan.LearningPlanVersionManager;
import com.aidevplanner.backend.learningplan.PlanAdjustAgentRequest;
import com.aidevplanner.backend.learningplan.PlanAdjustAgentResponse;
import com.aidevplanner.backend.learningplan.PlanAdjustTaskPayload;
import com.aidevplanner.backend.learningplan.PlanAdjusterClient;
import com.aidevplanner.backend.learningplan.PlanMovedTaskResponse;
import com.aidevplanner.backend.profile.UserProfileService;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressLogServiceTests {

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private DailyTaskRepository dailyTaskRepository;

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private ProgressLogRepository progressLogRepository;

    @Mock
    private PlanAdjusterClient planAdjusterClient;

    @Mock
    private ProgressReviewerClient progressReviewerClient;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private LearningPlanVersionManager learningPlanVersionManager;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ProgressLogService progressLogService;

    @BeforeEach
    void setUp() {
        progressLogService = new ProgressLogService(
                agentRunRepository,
                authenticatedUserService,
                dailyTaskRepository,
                learningPlanRepository,
                objectMapper,
                planAdjusterClient,
                progressLogRepository,
                progressReviewerClient,
                userProfileService,
                learningPlanVersionManager
        );
    }

    @Test
    void submitsProgressAndSyncsTaskStatuses() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        List<DailyTask> tasks = todayTasks(plan, goal);
        List<DailyTask> allTasks = allTasks(plan, goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(30L, 1))
                .thenReturn(tasks);
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L))
                .thenReturn(allTasks);
        when(progressLogRepository.findByPlanIdOrderByCreatedAtDesc(30L))
                .thenReturn(List.of(progressLog(plan, goal)));
        when(progressReviewerClient.review(any(ProgressReviewAgentRequest.class)))
                .thenReturn(AgentClientResponse.model(new ProgressReviewAgentResponse(
                        List.of("Create progress table"),
                        List.of("Create progress form"),
                        List.of("Need server verification"),
                        "minor",
                        "Finish the UI polish before adding new scope.",
                        List.of("完成了数据层工作"),
                        List.of("补齐表单交互"),
                        "keep",
                        "medium"
                )));
        when(planAdjusterClient.adjust(any(PlanAdjustAgentRequest.class)))
                .thenReturn(AgentClientResponse.fallback(new PlanAdjustAgentResponse(
                        List.of(new PlanAdjustTaskPayload(
                                null,
                                2,
                                null,
                                "Carry over: Create progress form",
                                "Build frontend submit form.",
                                60,
                                "build",
                                "Progress form",
                                "high",
                                DailyTaskStatus.PENDING
                        )),
                        List.of(new PlanMovedTaskResponse(
                                2L,
                                "Create progress form",
                                1,
                                2,
                                "The task was unfinished."
                        )),
                        List.of(),
                        "Tomorrow's plan was adjusted to carry over unfinished work."
                )));
        when(progressLogRepository.save(any(ProgressLog.class)))
                .thenAnswer(invocation -> {
                    ProgressLog log = invocation.getArgument(0);
                    ReflectionTestUtils.setField(log, "id", 100L);
                    ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 6, 9, 14, 0));
                    ReflectionTestUtils.setField(log, "updatedAt", LocalDateTime.of(2026, 6, 9, 14, 0));
                    return log;
                });

        ProgressLogResponse response = progressLogService.submitProgress(new ProgressSubmitRequest(
                30L,
                1,
                "Finished the API and still need more UI polish.",
                List.of(1L),
                List.of(2L),
                List.of("Need server verification")
        ));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.planId()).isEqualTo(30L);
        assertThat(response.goalId()).isEqualTo(10L);
        assertThat(response.completedTaskIds()).containsExactly(1L);
        assertThat(response.unfinishedTaskIds()).containsExactly(2L);
        assertThat(response.blockers()).containsExactly("Need server verification");
        assertThat(response.reviewResultJson())
                .containsEntry("impact", "minor")
                .containsEntry("suggestion", "Finish the UI polish before adding new scope.")
                .containsEntry("paceAdjustment", "keep")
                .containsEntry("confidence", "medium");
        assertThat(response.reviewResultJson()).containsKey("planAdjustment");
        assertThat(response.reviewResultJson()).containsKey("adaptiveSchedule");
        assertThat(tasks.get(0).getStatus()).isEqualTo(DailyTaskStatus.DONE);
        assertThat(tasks.get(1).getStatus()).isEqualTo(DailyTaskStatus.SKIPPED);
        assertThat(allTasks.stream()
                .filter(task -> task.getDayIndex() == 2)
                .map(DailyTask::getEstimatedMinutes))
                .contains(72);

        ArgumentCaptor<ProgressLog> logCaptor = ArgumentCaptor.forClass(ProgressLog.class);
        verify(progressLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getUserFeedback())
                .isEqualTo("Finished the API and still need more UI polish.");
        assertThat(logCaptor.getValue().getReviewResultJson()).containsEntry("impact", "minor");
        assertThat(plan.getPlanJson()).containsKey("adjustmentHistory");

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(2)).save(runCaptor.capture());
        List<AgentRun> savedRuns = runCaptor.getAllValues();
        assertThat(savedRuns).extracting(AgentRun::getAgentName)
                .containsExactly("Progress Reviewer", "Plan Adjuster");
        assertThat(savedRuns).extracting(AgentRun::getStatus)
                .containsExactly(AgentRunStatus.SUCCESS, AgentRunStatus.SUCCESS);
        assertThat(savedRuns).extracting(AgentRun::getResponseSource)
                .containsExactly(AgentResponseSource.MODEL, AgentResponseSource.FALLBACK);
        assertThat(savedRuns.get(0).getInputJson()).contains("\"dayIndex\":1");
        assertThat(savedRuns.get(0).getOutputJson()).contains("\"impact\":\"minor\"");
        assertThat(savedRuns.get(1).getInputJson()).contains("\"currentDayIndex\":1");
        assertThat(savedRuns.get(1).getOutputJson()).contains("\"movedTasks\"");
    }

    @Test
    void rejectsTaskIdsOutsideRequestedDay() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(30L, 1))
                .thenReturn(todayTasks(plan, goal));

        assertThatThrownBy(() -> progressLogService.submitProgress(new ProgressSubmitRequest(
                30L,
                1,
                "Tried to submit an invalid task.",
                List.of(99L),
                List.of(),
                List.of()
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Daily task in learning plan day 99 was not found.");
    }

    @Test
    void listsProgressForPlanAndDay() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        ProgressLog log = progressLog(plan, goal);
        when(learningPlanRepository.existsById(30L)).thenReturn(true);
        when(progressLogRepository.findByPlanIdAndDayIndexOrderByCreatedAtDesc(30L, 1))
                .thenReturn(List.of(log));

        List<ProgressLogResponse> response = progressLogService.listProgress(30L, 1);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).dayIndex()).isEqualTo(1);
        assertThat(response.get(0).completedTaskIds()).containsExactly(1L);
    }

    @Test
    void returnsAdaptiveScheduleControl() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        ProgressLog log = progressLog(plan, goal);
        ReflectionTestUtils.setField(plan, "planJson", Map.of(
                "adaptiveScheduleOverride", Map.of(
                        "pacing", "lighter",
                        "reason", "User requested a lighter load.",
                        "affectedDayIndexes", List.of(2, 3),
                        "anchorDayIndex", 1,
                        "appliedAt", "2026-06-13T10:00:00"
                )
        ));
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(progressLogRepository.findByPlanIdOrderByCreatedAtDesc(30L)).thenReturn(List.of(log));

        AdaptiveScheduleControlResponse response = progressLogService.getAdaptiveScheduleControl(30L);

        assertThat(response.latestAutomatic()).isNotNull();
        assertThat(response.latestAutomatic().pacing()).isEqualTo("steady");
        assertThat(response.activeOverride()).isNotNull();
        assertThat(response.activeOverride().pacing()).isEqualTo("lighter");
        assertThat(response.evidence()).isNotEmpty();
    }

    @Test
    void appliesManualAdaptiveScheduleOverride() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        List<DailyTask> allTasks = allTasks(plan, goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(progressLogRepository.findByPlanIdOrderByCreatedAtDesc(30L)).thenReturn(List.of(progressLog(plan, goal)));
        when(dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(30L)).thenReturn(allTasks);

        AdaptiveScheduleControlResponse response = progressLogService.overrideAdaptiveSchedule(
                30L,
                new AdaptiveScheduleOverrideRequest("lighter", "Need lighter workload this week.")
        );

        assertThat(response.activeOverride()).isNotNull();
        assertThat(response.activeOverride().pacing()).isEqualTo("lighter");
        assertThat(response.activeOverride().affectedDayIndexes()).contains(2, 3);
        assertThat(allTasks.stream()
                .filter(task -> task.getDayIndex() == 2)
                .map(DailyTask::getEstimatedMinutes))
                .contains(72);
    }

    private ProgressLog progressLog(LearningPlan plan, Goal goal) {
        ProgressLog log = new ProgressLog(
                plan,
                goal.getUser(),
                goal,
                1,
                "Submitted daily progress.",
                List.of(1L),
                List.of(2L),
                List.of("Waiting for deploy"),
                Map.of(
                        "impact", "medium",
                        "suggestion", "Clear the deploy blocker first.",
                        "paceAdjustment", "slower",
                        "confidence", "low"
                )
        );
        ReflectionTestUtils.setField(log, "id", 100L);
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 6, 9, 14, 0));
        ReflectionTestUtils.setField(log, "updatedAt", LocalDateTime.of(2026, 6, 9, 14, 0));
        return log;
    }

    private LearningPlan learningPlan(Goal goal) {
        LearningPlan plan = new LearningPlan(
                goal.getUser(),
                goal,
                null,
                "21-Day AI Planner MVP Plan",
                21,
                Map.of("planTitle", "21-Day AI Planner MVP Plan")
        );
        ReflectionTestUtils.setField(plan, "id", 30L);
        return plan;
    }

    private List<DailyTask> tasks(LearningPlan plan, Goal goal) {
        return allTasks(plan, goal).stream()
                .filter(task -> task.getDayIndex() == 1)
                .toList();
    }

    private List<DailyTask> todayTasks(LearningPlan plan, Goal goal) {
        DailyTask first = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                1,
                1,
                "Progress module",
                "Create progress table",
                "Add migration and entity.",
                30,
                "build",
                "progress_logs migration",
                "high"
        );
        DailyTask second = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                1,
                2,
                "Progress module",
                "Create progress form",
                "Build frontend submit form.",
                60,
                "build",
                "Progress form",
                "high"
        );
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        second.setStatus(DailyTaskStatus.IN_PROGRESS);
        return List.of(first, second);
    }

    private List<DailyTask> allTasks(LearningPlan plan, Goal goal) {
        List<DailyTask> dayOne = todayTasks(plan, goal);
        DailyTask third = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                2,
                1,
                "Progress module",
                "Validate progress flow",
                "Run integrated verification.",
                90,
                "verify",
                "Verification notes",
                "high"
        );
        DailyTask fourth = new DailyTask(
                plan,
                goal.getUser(),
                goal,
                3,
                1,
                "Progress module",
                "Refine progress UX",
                "Improve the feedback UI.",
                60,
                "improve",
                "Updated UI",
                "medium"
        );
        ReflectionTestUtils.setField(third, "id", 3L);
        ReflectionTestUtils.setField(fourth, "id", 4L);
        return List.of(dayOne.get(0), dayOne.get(1), third, fourth);
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
}
