package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.learningplan.DailyTask;
import com.aidevplanner.backend.learningplan.DailyTaskRepository;
import com.aidevplanner.backend.learningplan.DailyTaskStatus;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.learningplan.LearningPlanRepository;
import com.aidevplanner.backend.user.User;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressLogServiceTests {

    @Mock
    private DailyTaskRepository dailyTaskRepository;

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private ProgressLogRepository progressLogRepository;

    private ProgressLogService progressLogService;

    @BeforeEach
    void setUp() {
        progressLogService = new ProgressLogService(
                dailyTaskRepository,
                learningPlanRepository,
                progressLogRepository
        );
    }

    @Test
    void submitsProgressAndSyncsTaskStatuses() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        List<DailyTask> tasks = tasks(plan, goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(30L, 1))
                .thenReturn(tasks);
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
        assertThat(tasks.get(0).getStatus()).isEqualTo(DailyTaskStatus.DONE);
        assertThat(tasks.get(1).getStatus()).isEqualTo(DailyTaskStatus.PENDING);

        ArgumentCaptor<ProgressLog> logCaptor = ArgumentCaptor.forClass(ProgressLog.class);
        verify(progressLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getUserFeedback())
                .isEqualTo("Finished the API and still need more UI polish.");
    }

    @Test
    void rejectsTaskIdsOutsideRequestedDay() {
        Goal goal = goal();
        LearningPlan plan = learningPlan(goal);
        when(learningPlanRepository.findById(30L)).thenReturn(Optional.of(plan));
        when(dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(30L, 1))
                .thenReturn(tasks(plan, goal));

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
                Map.of()
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
