package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.learningplan.DailyTask;
import com.aidevplanner.backend.learningplan.DailyTaskRepository;
import com.aidevplanner.backend.learningplan.DailyTaskStatus;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.learningplan.LearningPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProgressLogService {

    private final DailyTaskRepository dailyTaskRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ProgressLogRepository progressLogRepository;

    public ProgressLogService(
            DailyTaskRepository dailyTaskRepository,
            LearningPlanRepository learningPlanRepository,
            ProgressLogRepository progressLogRepository
    ) {
        this.dailyTaskRepository = dailyTaskRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.progressLogRepository = progressLogRepository;
    }

    @Transactional
    public ProgressLogResponse submitProgress(ProgressSubmitRequest request) {
        LearningPlan plan = learningPlanRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", request.planId()));
        List<DailyTask> dayTasks =
                dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(
                        request.planId(),
                        request.dayIndex()
                );
        if (dayTasks.isEmpty()) {
            throw new ResourceNotFoundException("Learning plan day", request.dayIndex().longValue());
        }

        List<Long> completedTaskIds = normalizeIds(request.completedTaskIds());
        List<Long> unfinishedTaskIds = normalizeIds(request.unfinishedTaskIds()).stream()
                .filter(taskId -> !completedTaskIds.contains(taskId))
                .toList();
        validateTaskIds(dayTasks, completedTaskIds);
        validateTaskIds(dayTasks, unfinishedTaskIds);
        syncTaskStatuses(dayTasks, completedTaskIds, unfinishedTaskIds);

        ProgressLog savedLog = progressLogRepository.save(new ProgressLog(
                plan,
                plan.getUser(),
                plan.getGoal(),
                request.dayIndex(),
                request.userFeedback().trim(),
                completedTaskIds,
                unfinishedTaskIds,
                normalizeBlockers(request.blockers()),
                Map.of()
        ));

        return toResponse(savedLog);
    }

    @Transactional(readOnly = true)
    public List<ProgressLogResponse> listProgress(Long planId, Integer dayIndex) {
        if (!learningPlanRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Learning plan", planId);
        }

        List<ProgressLog> logs = dayIndex == null
                ? progressLogRepository.findByPlanIdOrderByCreatedAtDesc(planId)
                : progressLogRepository.findByPlanIdAndDayIndexOrderByCreatedAtDesc(planId, dayIndex);
        return logs.stream()
                .map(this::toResponse)
                .toList();
    }

    private void syncTaskStatuses(
            List<DailyTask> dayTasks,
            List<Long> completedTaskIds,
            List<Long> unfinishedTaskIds
    ) {
        for (DailyTask task : dayTasks) {
            if (completedTaskIds.contains(task.getId())) {
                task.setStatus(DailyTaskStatus.DONE);
            } else if (unfinishedTaskIds.contains(task.getId())) {
                task.setStatus(DailyTaskStatus.PENDING);
            }
        }
    }

    private void validateTaskIds(List<DailyTask> dayTasks, List<Long> taskIds) {
        Set<Long> validTaskIds = new LinkedHashSet<>();
        for (DailyTask task : dayTasks) {
            validTaskIds.add(task.getId());
        }

        for (Long taskId : taskIds) {
            if (!validTaskIds.contains(taskId)) {
                throw new ResourceNotFoundException("Daily task in learning plan day", taskId);
            }
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }

        Set<Long> normalizedIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                normalizedIds.add(id);
            }
        }
        return normalizedIds.stream().toList();
    }

    private List<String> normalizeBlockers(List<String> blockers) {
        if (blockers == null) {
            return List.of();
        }

        return blockers.stream()
                .filter(blocker -> blocker != null && !blocker.isBlank())
                .map(String::trim)
                .toList();
    }

    private ProgressLogResponse toResponse(ProgressLog log) {
        return new ProgressLogResponse(
                log.getId(),
                log.getPlan().getId(),
                log.getGoal().getId(),
                log.getUser().getId(),
                log.getDayIndex(),
                log.getUserFeedback(),
                log.getCompletedTaskIds() == null ? List.of() : log.getCompletedTaskIds(),
                log.getUnfinishedTaskIds() == null ? List.of() : log.getUnfinishedTaskIds(),
                log.getBlockers() == null ? List.of() : log.getBlockers(),
                log.getReviewResultJson() == null ? Map.of() : log.getReviewResultJson(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
