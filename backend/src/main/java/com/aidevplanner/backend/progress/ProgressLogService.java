package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.learningplan.DailyTask;
import com.aidevplanner.backend.learningplan.DailyTaskRepository;
import com.aidevplanner.backend.learningplan.DailyTaskStatus;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.learningplan.LearningPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProgressLogService {

    static final String AGENT_NAME = "Progress Reviewer";

    private final AgentRunRepository agentRunRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;
    private final ProgressLogRepository progressLogRepository;
    private final ProgressReviewerClient progressReviewerClient;

    public ProgressLogService(
            AgentRunRepository agentRunRepository,
            DailyTaskRepository dailyTaskRepository,
            LearningPlanRepository learningPlanRepository,
            ObjectMapper objectMapper,
            ProgressLogRepository progressLogRepository,
            ProgressReviewerClient progressReviewerClient
    ) {
        this.agentRunRepository = agentRunRepository;
        this.dailyTaskRepository = dailyTaskRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.objectMapper = objectMapper;
        this.progressLogRepository = progressLogRepository;
        this.progressReviewerClient = progressReviewerClient;
    }

    @Transactional(noRollbackFor = AgentServiceException.class)
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
        List<String> blockers = normalizeBlockers(request.blockers());
        String userFeedback = request.userFeedback().trim();

        ProgressReviewAgentRequest reviewRequest = buildReviewRequest(
                request.dayIndex(),
                dayTasks,
                userFeedback,
                completedTaskIds,
                unfinishedTaskIds,
                blockers
        );
        String inputJson = writeJson(reviewRequest);
        long startedAt = System.nanoTime();
        ProgressReviewAgentResponse reviewResponse;

        try {
            reviewResponse = normalizeReviewResponse(progressReviewerClient.review(reviewRequest));
            String outputJson = writeJson(reviewResponse);
            agentRunRepository.save(new AgentRun(
                    plan.getUser(),
                    plan.getGoal(),
                    AGENT_NAME,
                    inputJson,
                    outputJson,
                    AgentRunStatus.SUCCESS,
                    elapsedMs(startedAt),
                    null
            ));
        } catch (AgentServiceException exception) {
            agentRunRepository.save(new AgentRun(
                    plan.getUser(),
                    plan.getGoal(),
                    AGENT_NAME,
                    inputJson,
                    null,
                    AgentRunStatus.FAILED,
                    elapsedMs(startedAt),
                    exception.getMessage()
            ));
            throw exception;
        }

        ProgressLog savedLog = progressLogRepository.save(new ProgressLog(
                plan,
                plan.getUser(),
                plan.getGoal(),
                request.dayIndex(),
                userFeedback,
                completedTaskIds,
                unfinishedTaskIds,
                blockers,
                toReviewResultJson(reviewResponse)
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

    private ProgressReviewAgentRequest buildReviewRequest(
            Integer dayIndex,
            List<DailyTask> dayTasks,
            String userFeedback,
            List<Long> completedTaskIds,
            List<Long> unfinishedTaskIds,
            List<String> blockers
    ) {
        return new ProgressReviewAgentRequest(
                dayIndex,
                dayTasks.stream().map(this::toReviewTask).toList(),
                userFeedback,
                selectTasks(dayTasks, completedTaskIds),
                selectTasks(dayTasks, unfinishedTaskIds),
                blockers
        );
    }

    private List<ProgressReviewTaskPayload> selectTasks(List<DailyTask> dayTasks, List<Long> taskIds) {
        return dayTasks.stream()
                .filter(task -> taskIds.contains(task.getId()))
                .map(this::toReviewTask)
                .toList();
    }

    private ProgressReviewTaskPayload toReviewTask(DailyTask task) {
        return new ProgressReviewTaskPayload(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getEstimatedMinutes(),
                task.getType(),
                task.getDeliverable(),
                task.getPriority()
        );
    }

    private ProgressReviewAgentResponse normalizeReviewResponse(ProgressReviewAgentResponse response) {
        return new ProgressReviewAgentResponse(
                cleanList(response.completedTasks()),
                cleanList(response.unfinishedTasks()),
                cleanList(response.blockers()),
                normalizeImpact(response.impact()),
                firstPresent(response.suggestion(), "Review today's blockers and keep tomorrow focused.")
        );
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

    private Map<String, Object> toReviewResultJson(ProgressReviewAgentResponse response) {
        return objectMapper.convertValue(response, new TypeReference<>() {
        });
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

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private String normalizeImpact(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.equals("none") || normalized.equals("minor")
                || normalized.equals("medium") || normalized.equals("major")) {
            return normalized;
        }
        return "medium";
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Unable to serialize progress reviewer payload.", exception);
        }
    }

    private long elapsedMs(long startedAt) {
        return Math.max(1, (System.nanoTime() - startedAt) / 1_000_000);
    }
}
