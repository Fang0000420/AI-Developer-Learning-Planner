package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
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
import com.aidevplanner.backend.learningplan.LearningPlanVersionManager;
import com.aidevplanner.backend.learningplan.PlanAdjustAgentRequest;
import com.aidevplanner.backend.learningplan.PlanAdjustAgentResponse;
import com.aidevplanner.backend.learningplan.PlanAdjustDayPayload;
import com.aidevplanner.backend.learningplan.PlanAdjustReviewPayload;
import com.aidevplanner.backend.learningplan.PlanAdjustTaskPayload;
import com.aidevplanner.backend.learningplan.PlanAdjusterClient;
import com.aidevplanner.backend.learningplan.PlanMovedTaskResponse;
import com.aidevplanner.backend.learningplan.PlanSplitTaskResponse;
import com.aidevplanner.backend.profile.UserProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProgressLogService {

    static final String AGENT_NAME = "Progress Reviewer";
    static final String PLAN_ADJUSTER_AGENT_NAME = "Plan Adjuster";

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final DailyTaskRepository dailyTaskRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;
    private final PlanAdjusterClient planAdjusterClient;
    private final ProgressLogRepository progressLogRepository;
    private final ProgressReviewerClient progressReviewerClient;
    private final UserProfileService userProfileService;
    private final LearningPlanVersionManager learningPlanVersionManager;

    public ProgressLogService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            DailyTaskRepository dailyTaskRepository,
            LearningPlanRepository learningPlanRepository,
            ObjectMapper objectMapper,
            PlanAdjusterClient planAdjusterClient,
            ProgressLogRepository progressLogRepository,
            ProgressReviewerClient progressReviewerClient,
            UserProfileService userProfileService,
            LearningPlanVersionManager learningPlanVersionManager
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.dailyTaskRepository = dailyTaskRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.objectMapper = objectMapper;
        this.planAdjusterClient = planAdjusterClient;
        this.progressLogRepository = progressLogRepository;
        this.progressReviewerClient = progressReviewerClient;
        this.userProfileService = userProfileService;
        this.learningPlanVersionManager = learningPlanVersionManager;
    }

    @Transactional(noRollbackFor = AgentServiceException.class)
    public ProgressLogResponse submitProgress(ProgressSubmitRequest request) {
        LearningPlan plan = learningPlanRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", request.planId()));
        ensureCurrentUserOwns(plan);
        List<DailyTask> dayTasks =
                dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(
                        request.planId(),
                        request.dayIndex()
                );
        if (dayTasks.isEmpty()) {
            throw new ResourceNotFoundException("Learning plan day", request.dayIndex().longValue());
        }
        List<DailyTask> allPlanTasks =
                dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(request.planId());

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
                plan,
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
            AgentClientResponse<ProgressReviewAgentResponse> reviewClientResponse =
                    progressReviewerClient.review(reviewRequest);
            reviewResponse = normalizeReviewResponse(reviewClientResponse.payload(), plan);
            String outputJson = writeJson(reviewResponse);
            AgentRun reviewRun = new AgentRun(
                    plan.getUser(),
                    plan.getGoal(),
                    AGENT_NAME,
                    inputJson,
                    outputJson,
                    AgentRunStatus.SUCCESS,
                    elapsedMs(startedAt),
                    reviewClientResponse.responseSource(),
                    null
            );
            reviewRun.setPlan(plan);
            agentRunRepository.save(reviewRun);
        } catch (AgentServiceException exception) {
            AgentRun failedReviewRun = new AgentRun(
                    plan.getUser(),
                    plan.getGoal(),
                    AGENT_NAME,
                    inputJson,
                    null,
                    AgentRunStatus.FAILED,
                    elapsedMs(startedAt),
                    exception.getMessage()
            );
            failedReviewRun.setPlan(plan);
            agentRunRepository.save(failedReviewRun);
            throw exception;
        }

        Integer targetDayIndex = nextDayIndex(plan, request.dayIndex());
        PlanAdjustAgentRequest adjustRequest = buildAdjustRequest(
                plan,
                request.dayIndex(),
                targetDayIndex,
                allPlanTasks,
                dayTasks,
                reviewResponse,
                unfinishedTaskIds
        );
        String adjustInputJson = writeJson(adjustRequest);
        long adjustStartedAt = System.nanoTime();
        PlanAdjustAgentResponse adjustResponse;

        try {
            AgentClientResponse<PlanAdjustAgentResponse> adjustClientResponse =
                    planAdjusterClient.adjust(adjustRequest);
            adjustResponse = normalizeAdjustResponse(adjustClientResponse.payload(), plan);
            applyPlanAdjustment(
                    plan,
                    targetDayIndex,
                    dayTasks,
                    allPlanTasks,
                    unfinishedTaskIds,
                    adjustResponse
            );
            appendAdjustmentHistory(plan, request.dayIndex(), targetDayIndex, adjustResponse);
            String adjustOutputJson = writeJson(adjustResponse);
            AgentRun adjustRun = new AgentRun(
                    plan.getUser(),
                    plan.getGoal(),
                    PLAN_ADJUSTER_AGENT_NAME,
                    adjustInputJson,
                    adjustOutputJson,
                    AgentRunStatus.SUCCESS,
                    elapsedMs(adjustStartedAt),
                    adjustClientResponse.responseSource(),
                    null
            );
            adjustRun.setPlan(plan);
            agentRunRepository.save(adjustRun);
        } catch (AgentServiceException exception) {
            AgentRun failedAdjustRun = new AgentRun(
                    plan.getUser(),
                    plan.getGoal(),
                    PLAN_ADJUSTER_AGENT_NAME,
                    adjustInputJson,
                    null,
                    AgentRunStatus.FAILED,
                    elapsedMs(adjustStartedAt),
                    exception.getMessage()
            );
            failedAdjustRun.setPlan(plan);
            agentRunRepository.save(failedAdjustRun);
            throw exception;
        }

        Map<String, Object> reviewResultJson = toReviewResultJson(reviewResponse);
        reviewResultJson.put("planAdjustment", toPlanAdjustmentJson(adjustResponse));
        AdaptiveScheduleResult adaptiveSchedule = applyAdaptiveScheduling(
                plan,
                request.dayIndex(),
                completedTaskIds,
                unfinishedTaskIds,
                blockers,
                reviewResponse
        );
        reviewResultJson.put("adaptiveSchedule", toAdaptiveScheduleJson(adaptiveSchedule));
        List<DailyTask> updatedTasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(plan.getId());
        learningPlanVersionManager.captureSnapshot(
                plan,
                updatedTasks,
                "progress_adjustment",
                firstPresent(adjustResponse.reason(), adaptiveSchedule.reason()),
                adaptiveSchedule.affectedDayIndexes()
        );
        userProfileService.applyProgressFeedback(plan, reviewResponse, request);
        ProgressLog savedLog = progressLogRepository.save(new ProgressLog(
                plan,
                plan.getUser(),
                plan.getGoal(),
                request.dayIndex(),
                userFeedback,
                completedTaskIds,
                unfinishedTaskIds,
                blockers,
                reviewResultJson
        ));

        return toResponse(savedLog);
    }

    @Transactional(readOnly = true)
    public List<ProgressLogResponse> listProgress(Long planId, Integer dayIndex) {
        ensureCanReadPlan(planId);

        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        List<ProgressLog> logs;
        if (currentUserId == null) {
            logs = dayIndex == null
                    ? progressLogRepository.findByPlanIdOrderByCreatedAtDesc(planId)
                    : progressLogRepository.findByPlanIdAndDayIndexOrderByCreatedAtDesc(planId, dayIndex);
        } else {
            logs = dayIndex == null
                    ? progressLogRepository.findByPlanIdAndUserIdOrderByCreatedAtDesc(planId, currentUserId)
                    : progressLogRepository.findByPlanIdAndUserIdAndDayIndexOrderByCreatedAtDesc(
                            planId,
                            currentUserId,
                            dayIndex
                    );
        }
        return logs.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdaptiveScheduleControlResponse getAdaptiveScheduleControl(Long planId) {
        LearningPlan plan = learningPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", planId));
        ensureCurrentUserOwns(plan);
        List<ProgressLog> logs = progressLogRepository.findByPlanIdOrderByCreatedAtDesc(planId);
        return buildAdaptiveScheduleControl(plan, logs == null ? List.of() : logs);
    }

    @Transactional
    public AdaptiveScheduleControlResponse overrideAdaptiveSchedule(
            Long planId,
            AdaptiveScheduleOverrideRequest request
    ) {
        LearningPlan plan = learningPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", planId));
        ensureCurrentUserOwns(plan);
        List<ProgressLog> logs = progressLogRepository.findByPlanIdOrderByCreatedAtDesc(planId);
        List<ProgressLog> safeLogs = logs == null ? List.of() : logs;
        Integer anchorDayIndex = safeLogs.stream()
                .findFirst()
                .map(ProgressLog::getDayIndex)
                .orElse(1);
        List<DailyTask> allTasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId);
        List<DailyTask> tasksToAdjust = allTasks.stream()
                .filter(task -> task.getDayIndex() > anchorDayIndex)
                .filter(task -> task.getDayIndex() <= anchorDayIndex + 2)
                .filter(task -> task.getStatus() == DailyTaskStatus.PENDING)
                .sorted(Comparator.comparing(DailyTask::getDayIndex).thenComparing(DailyTask::getTaskOrder))
                .toList();
        applyPacingToTasks(tasksToAdjust, request.pacing());
        AdaptiveScheduleOverrideSummary override = new AdaptiveScheduleOverrideSummary(
                request.pacing(),
                firstPresent(request.reason(), defaultOverrideReason(plan, request.pacing())),
                tasksToAdjust.stream().map(DailyTask::getDayIndex).distinct().toList(),
                anchorDayIndex,
                LocalDateTime.now()
        );
        saveAdaptiveScheduleOverride(plan, override);
        List<DailyTask> refreshedTasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId);
        learningPlanVersionManager.captureSnapshot(
                plan,
                refreshedTasks,
                "manual_override",
                override.reason(),
                override.affectedDayIndexes()
        );
        return new AdaptiveScheduleControlResponse(
                latestAutomaticResult(plan, safeLogs),
                override,
                adaptiveEvidence(plan, safeLogs)
        );
    }

    private void ensureCanReadPlan(Long planId) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        boolean exists = currentUserId == null
                ? learningPlanRepository.existsById(planId)
                : learningPlanRepository.existsByIdAndUserId(planId, currentUserId);
        if (!exists) {
            throw new ResourceNotFoundException("Learning plan", planId);
        }
    }

    private AdaptiveScheduleControlResponse buildAdaptiveScheduleControl(
            LearningPlan plan,
            List<ProgressLog> logs
    ) {
        return new AdaptiveScheduleControlResponse(
                latestAutomaticResult(plan, logs),
                activeOverride(plan),
                adaptiveEvidence(plan, logs)
        );
    }

    private void ensureCurrentUserOwns(LearningPlan plan) {
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!plan.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Learning plan", plan.getId());
            }
        });
    }

    private ProgressReviewAgentRequest buildReviewRequest(
            LearningPlan plan,
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
                blockers,
                plan.getGoal().getResponseLanguage().name()
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

    private PlanAdjustAgentRequest buildAdjustRequest(
            LearningPlan plan,
            Integer currentDayIndex,
            Integer targetDayIndex,
            List<DailyTask> allPlanTasks,
            List<DailyTask> todayTasks,
            ProgressReviewAgentResponse reviewResponse,
            List<Long> unfinishedTaskIds
    ) {
        return new PlanAdjustAgentRequest(
                plan.getId(),
                currentDayIndex,
                toAdjustDays(allPlanTasks),
                todayTasks.stream().map(this::toAdjustTask).toList(),
                new PlanAdjustReviewPayload(
                        cleanList(reviewResponse.completedTasks()),
                        cleanList(reviewResponse.unfinishedTasks()),
                        cleanList(reviewResponse.blockers()),
                        normalizeImpact(reviewResponse.impact()),
                        firstPresent(reviewResponse.suggestion(), "Review the unfinished work first.")
                ),
                todayTasks.stream()
                        .filter(task -> unfinishedTaskIds.contains(task.getId()))
                        .map(this::toAdjustTask)
                        .toList(),
                allPlanTasks.stream()
                        .filter(task -> targetDayIndex.equals(task.getDayIndex()))
                        .map(this::toAdjustTask)
                        .toList(),
                plan.getGoal().getResponseLanguage().name()
        );
    }

    private List<PlanAdjustDayPayload> toAdjustDays(List<DailyTask> tasks) {
        Map<Integer, List<DailyTask>> byDay = tasks.stream()
                .collect(Collectors.groupingBy(
                        DailyTask::getDayIndex,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return byDay.entrySet().stream()
                .map(entry -> new PlanAdjustDayPayload(
                        entry.getKey(),
                        entry.getValue().isEmpty() ? "" : entry.getValue().get(0).getDayTheme(),
                        entry.getValue().stream().map(this::toAdjustTask).toList()
                ))
                .toList();
    }

    private PlanAdjustTaskPayload toAdjustTask(DailyTask task) {
        return new PlanAdjustTaskPayload(
                task.getId(),
                task.getDayIndex(),
                task.getTaskOrder(),
                task.getTitle(),
                task.getDescription(),
                task.getEstimatedMinutes(),
                task.getType(),
                task.getDeliverable(),
                task.getPriority(),
                task.getStatus()
        );
    }

    private ProgressReviewAgentResponse normalizeReviewResponse(
            ProgressReviewAgentResponse response,
            LearningPlan plan
    ) {
        boolean zh = isZh(plan);
        return new ProgressReviewAgentResponse(
                cleanList(response.completedTasks()),
                cleanList(response.unfinishedTasks()),
                cleanList(response.blockers()),
                normalizeImpact(response.impact()),
                firstPresent(
                        response.suggestion(),
                        zh ? "复盘今天的阻塞，并让明天保持聚焦。" : "Review today's blockers and keep tomorrow focused."
                ),
                cleanList(response.wins()),
                cleanList(response.nextFocus()),
                normalizePaceAdjustment(response.paceAdjustment()),
                normalizeConfidence(response.confidence())
        );
    }

    private PlanAdjustAgentResponse normalizeAdjustResponse(PlanAdjustAgentResponse response, LearningPlan plan) {
        boolean zh = isZh(plan);
        if (response == null) {
            return new PlanAdjustAgentResponse(
                    List.of(),
                    List.of(),
                    List.of(),
                    zh ? "未返回计划调整结果。" : "No plan adjustment was returned."
            );
        }

        return new PlanAdjustAgentResponse(
                response.nextDayTasks() == null ? List.of() : response.nextDayTasks(),
                response.movedTasks() == null ? List.of() : response.movedTasks().stream()
                        .map(task -> new PlanMovedTaskResponse(
                                task.taskId(),
                                firstPresent(task.title(), zh ? "移动任务" : "Moved task"),
                                positiveOrDefault(task.fromDayIndex(), 1),
                                positiveOrDefault(task.toDayIndex(), 1),
                                firstPresent(
                                        task.reason(),
                                        zh ? "该任务因未完成而移动。" : "The task was moved because it was unfinished."
                                )
                        ))
                        .toList(),
                response.splitTasks() == null ? List.of() : response.splitTasks().stream()
                        .map(task -> new PlanSplitTaskResponse(
                                task.sourceTaskId(),
                                firstPresent(task.sourceTitle(), zh ? "拆分任务" : "Split task"),
                                task.parts() == null ? List.of() : task.parts(),
                                firstPresent(
                                        task.reason(),
                                        zh
                                                ? "该任务被拆分成下一天更小的部分。"
                                                : "The task was split into smaller next-day parts."
                                )
                        ))
                        .toList(),
                firstPresent(
                        response.reason(),
                        zh
                                ? "明天计划已根据今天的进度进行调整。"
                                : "Tomorrow's plan was adjusted based on today's progress."
                )
        );
    }

    private void applyPlanAdjustment(
            LearningPlan plan,
            Integer targetDayIndex,
            List<DailyTask> dayTasks,
            List<DailyTask> allPlanTasks,
            List<Long> unfinishedTaskIds,
            PlanAdjustAgentResponse adjustResponse
    ) {
        if (unfinishedTaskIds.isEmpty()) {
            return;
        }

        Set<Long> splitSourceIds = adjustResponse.splitTasks().stream()
                .map(PlanSplitTaskResponse::sourceTaskId)
                .filter(id -> id != null && unfinishedTaskIds.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DailyTask> newTasks = new ArrayList<>();
        int nextOrder = nextTaskOrder(allPlanTasks, newTasks, targetDayIndex);
        String targetTheme = targetDayTheme(plan, allPlanTasks, targetDayIndex);

        for (PlanSplitTaskResponse splitTask : adjustResponse.splitTasks()) {
            if (splitTask.sourceTaskId() == null || !unfinishedTaskIds.contains(splitTask.sourceTaskId())) {
                continue;
            }
            DailyTask sourceTask = findTask(dayTasks, splitTask.sourceTaskId());
            if (sourceTask == null) {
                continue;
            }
            sourceTask.setStatus(DailyTaskStatus.SKIPPED);
            for (PlanAdjustTaskPayload part : splitTask.parts()) {
                String title = firstPresent(part.title(), sourceTask.getTitle() + " - part");
                if (hasTaskWithTitle(allPlanTasks, newTasks, targetDayIndex, title)) {
                    continue;
                }
                newTasks.add(new DailyTask(
                        plan,
                        plan.getUser(),
                        plan.getGoal(),
                        targetDayIndex,
                        nextOrder++,
                        targetTheme,
                        title,
                        firstPresent(part.description(), sourceTask.getDescription()),
                        positiveOrDefault(part.estimatedMinutes(), Math.max(15, sourceTask.getEstimatedMinutes() / 2)),
                        firstPresent(part.type(), sourceTask.getType()),
                        firstPresent(part.deliverable(), sourceTask.getDeliverable()),
                        normalizePriority(part.priority())
                ));
            }
        }

        for (DailyTask task : dayTasks) {
            if (!unfinishedTaskIds.contains(task.getId()) || splitSourceIds.contains(task.getId())) {
                continue;
            }
            task.setStatus(DailyTaskStatus.SKIPPED);
            String title = (isZh(plan) ? "结转：" : "Carry over: ") + task.getTitle();
            if (hasTaskWithTitle(allPlanTasks, newTasks, targetDayIndex, title)) {
                continue;
            }
            newTasks.add(new DailyTask(
                    plan,
                    plan.getUser(),
                    plan.getGoal(),
                    targetDayIndex,
                    nextOrder++,
                    targetTheme,
                    title,
                    task.getDescription(),
                    task.getEstimatedMinutes(),
                    task.getType(),
                    task.getDeliverable(),
                    task.getPriority()
            ));
        }

        if (!newTasks.isEmpty()) {
            dailyTaskRepository.saveAll(newTasks);
        }
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
        return new LinkedHashMap<>(objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {
        }));
    }

    private Map<String, Object> toPlanAdjustmentJson(PlanAdjustAgentResponse response) {
        return new LinkedHashMap<>(objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {
        }));
    }

    private Map<String, Object> toAdaptiveScheduleJson(AdaptiveScheduleResult result) {
        return new LinkedHashMap<>(objectMapper.convertValue(result, new TypeReference<Map<String, Object>>() {
        }));
    }

    @SuppressWarnings("unchecked")
    private void appendAdjustmentHistory(
            LearningPlan plan,
            Integer currentDayIndex,
            Integer targetDayIndex,
            PlanAdjustAgentResponse adjustResponse
    ) {
        Map<String, Object> planJson = plan.getPlanJson() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(plan.getPlanJson());
        Object existingHistory = planJson.get("adjustmentHistory");
        List<Object> history = existingHistory instanceof List<?>
                ? new ArrayList<>((List<Object>) existingHistory)
                : new ArrayList<>();

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("currentDayIndex", currentDayIndex);
        entry.put("targetDayIndex", targetDayIndex);
        entry.put("reason", adjustResponse.reason());
        entry.put("movedTasks", objectMapper.convertValue(
                adjustResponse.movedTasks(),
                new TypeReference<List<Map<String, Object>>>() {
                }
        ));
        entry.put("splitTasks", objectMapper.convertValue(
                adjustResponse.splitTasks(),
                new TypeReference<List<Map<String, Object>>>() {
                }
        ));
        entry.put("createdAt", LocalDateTime.now().toString());
        history.add(entry);

        planJson.put("adjustmentHistory", history);
        plan.setPlanJson(planJson);
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

    private AdaptiveScheduleResult applyAdaptiveScheduling(
            LearningPlan plan,
            Integer currentDayIndex,
            List<Long> completedTaskIds,
            List<Long> unfinishedTaskIds,
            List<String> blockers,
            ProgressReviewAgentResponse reviewResponse
    ) {
        List<ProgressSignal> signals = new ArrayList<>();
        signals.add(new ProgressSignal(
                completionRate(completedTaskIds.size(), unfinishedTaskIds.size()),
                blockers.size(),
                normalizePaceAdjustment(reviewResponse.paceAdjustment()),
                normalizeImpact(reviewResponse.impact())
        ));
        List<ProgressLog> existingLogs = progressLogRepository.findByPlanIdOrderByCreatedAtDesc(plan.getId());
        if (existingLogs != null) {
            existingLogs.stream()
                    .limit(2)
                    .map(this::toProgressSignal)
                    .forEach(signals::add);
        }

        double averageCompletion = roundTwoDecimals(signals.stream()
                .mapToDouble(ProgressSignal::completionRate)
                .average()
                .orElse(0.0));
        double averageBlockers = roundTwoDecimals(signals.stream()
                .mapToInt(ProgressSignal::blockerCount)
                .average()
                .orElse(0.0));
        long slowerSignals = signals.stream()
                .filter(signal -> signal.paceAdjustment().equals("slower")
                        || signal.impact().equals("major")
                        || signal.impact().equals("medium"))
                .count();
        long fasterSignals = signals.stream()
                .filter(signal -> signal.paceAdjustment().equals("faster")
                        || signal.impact().equals("none"))
                .count();

        String pacing = "steady";
        int minuteAdjustmentPercent = 0;
        if (averageCompletion < 0.55 || averageBlockers >= 1.5 || slowerSignals >= 2) {
            pacing = "lighter";
            minuteAdjustmentPercent = -20;
        } else if (averageCompletion >= 0.85 && averageBlockers == 0.0 && fasterSignals >= 2) {
            pacing = "stronger";
            minuteAdjustmentPercent = 10;
        }

        List<DailyTask> tasksToAdjust = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(plan.getId()).stream()
                .filter(task -> task.getDayIndex() > currentDayIndex)
                .filter(task -> task.getDayIndex() <= currentDayIndex + 2)
                .filter(task -> task.getStatus() == DailyTaskStatus.PENDING)
                .sorted(Comparator.comparing(DailyTask::getDayIndex).thenComparing(DailyTask::getTaskOrder))
                .toList();
        LinkedHashSet<Integer> affectedDayIndexes = tasksToAdjust.stream()
                .map(DailyTask::getDayIndex)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (pacing.equals("lighter")) {
            applyPacingToTasks(tasksToAdjust, pacing);
        } else if (pacing.equals("stronger")) {
            applyPacingToTasks(tasksToAdjust, pacing);
        }

        return new AdaptiveScheduleResult(
                pacing,
                adaptiveReason(plan, pacing, averageCompletion, averageBlockers),
                averageCompletion,
                averageBlockers,
                minuteAdjustmentPercent,
                List.copyOf(affectedDayIndexes)
        );
    }

    private ProgressSignal toProgressSignal(ProgressLog log) {
        Object paceAdjustment = log.getReviewResultJson().get("paceAdjustment");
        Object impact = log.getReviewResultJson().get("impact");
        int completedCount = log.getCompletedTaskIds() == null ? 0 : log.getCompletedTaskIds().size();
        int unfinishedCount = log.getUnfinishedTaskIds() == null ? 0 : log.getUnfinishedTaskIds().size();
        int blockerCount = log.getBlockers() == null ? 0 : log.getBlockers().size();
        return new ProgressSignal(
                completionRate(completedCount, unfinishedCount),
                blockerCount,
                normalizePaceAdjustment(paceAdjustment == null ? null : String.valueOf(paceAdjustment)),
                normalizeImpact(impact == null ? null : String.valueOf(impact))
        );
    }

    private double completionRate(int completedCount, int unfinishedCount) {
        int total = completedCount + unfinishedCount;
        if (total <= 0) {
            return 0.0;
        }
        return (double) completedCount / total;
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String adaptiveReason(
            LearningPlan plan,
            String pacing,
            double averageCompletion,
            double averageBlockers
    ) {
        if (isZh(plan)) {
            return switch (pacing) {
                case "lighter" -> "最近几次反馈显示完成率偏低或阻塞偏多，后续两天任务已自动减负。";
                case "stronger" -> "最近几次反馈显示完成率稳定且阻塞较少，后续两天任务已适度加压。";
                default -> "最近几次反馈整体稳定，后续两天继续保持当前任务密度。";
            };
        }
        return switch (pacing) {
            case "lighter" -> "Recent feedback shows lower completion or more blockers, so the next two days were lightened.";
            case "stronger" -> "Recent feedback shows stable completion with few blockers, so the next two days were slightly intensified.";
            default -> "Recent feedback looks stable, so the next two days keep the current task density.";
        };
    }

    private String lowerPriority(String value) {
        return switch (normalizePriority(value)) {
            case "high" -> "medium";
            case "medium" -> "low";
            default -> "low";
        };
    }

    private String raisePriority(String value) {
        return switch (normalizePriority(value)) {
            case "low" -> "medium";
            case "medium" -> "high";
            default -> "high";
        };
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

    private Integer nextDayIndex(LearningPlan plan, Integer dayIndex) {
        Integer durationDays = plan.getDurationDays() == null ? dayIndex + 1 : plan.getDurationDays();
        return dayIndex < durationDays ? dayIndex + 1 : durationDays;
    }

    private int nextTaskOrder(
            List<DailyTask> allPlanTasks,
            List<DailyTask> newTasks,
            Integer targetDayIndex
    ) {
        int existingMax = allPlanTasks.stream()
                .filter(task -> targetDayIndex.equals(task.getDayIndex()))
                .mapToInt(DailyTask::getTaskOrder)
                .max()
                .orElse(0);
        return existingMax + newTasks.size() + 1;
    }

    private String targetDayTheme(LearningPlan plan, List<DailyTask> allPlanTasks, Integer targetDayIndex) {
        return allPlanTasks.stream()
                .filter(task -> targetDayIndex.equals(task.getDayIndex()))
                .map(DailyTask::getDayTheme)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(isZh(plan) ? "调整后的第 " + targetDayIndex + " 天" : "Adjusted Day " + targetDayIndex);
    }

    private DailyTask findTask(List<DailyTask> tasks, Long taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    private boolean hasTaskWithTitle(
            List<DailyTask> allPlanTasks,
            List<DailyTask> newTasks,
            Integer targetDayIndex,
            String title
    ) {
        return allPlanTasks.stream()
                .anyMatch(task -> targetDayIndex.equals(task.getDayIndex())
                        && task.getTitle().equalsIgnoreCase(title))
                || newTasks.stream()
                .anyMatch(task -> targetDayIndex.equals(task.getDayIndex())
                        && task.getTitle().equalsIgnoreCase(title));
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

    private boolean isZh(LearningPlan plan) {
        return plan != null
                && plan.getGoal() != null
                && "zh".equalsIgnoreCase(plan.getGoal().getResponseLanguage().name());
    }

    private Integer positiveOrDefault(Integer value, Integer defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String normalizePriority(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.equals("high") || normalized.equals("urgent") || normalized.equals("critical")) {
            return "high";
        }
        if (normalized.equals("low") || normalized.equals("optional")) {
            return "low";
        }
        return "medium";
    }

    private String normalizePaceAdjustment(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.equals("slower") || normalized.equals("faster") || normalized.equals("keep")) {
            return normalized;
        }
        return "keep";
    }

    private String normalizeConfidence(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.equals("low") || normalized.equals("medium") || normalized.equals("high")) {
            return normalized;
        }
        return "medium";
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

    private void applyPacingToTasks(List<DailyTask> tasksToAdjust, String pacing) {
        if ("lighter".equals(pacing)) {
            for (DailyTask task : tasksToAdjust) {
                task.setEstimatedMinutes(Math.max(15, (int) Math.round(task.getEstimatedMinutes() * 0.8)));
                if (task.getTaskOrder() != null && task.getTaskOrder() >= 3) {
                    task.setPriority(lowerPriority(task.getPriority()));
                }
            }
            return;
        }
        if ("stronger".equals(pacing)) {
            for (DailyTask task : tasksToAdjust) {
                if (task.getTaskOrder() != null && task.getTaskOrder() == 1) {
                    task.setEstimatedMinutes(Math.max(15, (int) Math.round(task.getEstimatedMinutes() * 1.1)));
                    task.setPriority(raisePriority(task.getPriority()));
                }
            }
        }
    }

    private AdaptiveScheduleResult latestAutomaticResult(LearningPlan plan, List<ProgressLog> logs) {
        if (logs.isEmpty()) {
            return new AdaptiveScheduleResult(
                    "steady",
                    isZh(plan)
                            ? "还没有足够的进度反馈，系统暂时保持当前任务密度。"
                            : "There is not enough progress feedback yet, so the system keeps the current task density.",
                    0.0,
                    0.0,
                    0,
                    List.of()
            );
        }
        Object value = logs.get(0).getReviewResultJson().get("adaptiveSchedule");
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) map;
            return objectMapper.convertValue(typedMap, AdaptiveScheduleResult.class);
        }
        return new AdaptiveScheduleResult(
                "steady",
                isZh(plan)
                        ? "最近一次反馈还未生成自动调度结果。"
                        : "The latest feedback did not produce an automatic schedule result.",
                0.0,
                0.0,
                0,
                List.of()
        );
    }

    private AdaptiveScheduleOverrideSummary activeOverride(LearningPlan plan) {
        if (plan.getPlanJson() == null) {
            return null;
        }
        Object value = plan.getPlanJson().get("adaptiveScheduleOverride");
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> typedMap = (Map<String, Object>) map;
        return objectMapper.convertValue(typedMap, AdaptiveScheduleOverrideSummary.class);
    }

    private List<String> adaptiveEvidence(LearningPlan plan, List<ProgressLog> logs) {
        List<String> values = new ArrayList<>();
        logs.stream().limit(3).forEach(log -> {
            if (log.getUserFeedback() != null && !log.getUserFeedback().isBlank()) {
                values.add((isZh(plan) ? "用户反馈：" : "User feedback: ") + log.getUserFeedback().trim());
            }
            if (log.getBlockers() != null && !log.getBlockers().isEmpty()) {
                values.add((isZh(plan) ? "阻塞项：" : "Blockers: ") + String.join("；", log.getBlockers()));
            }
            Object suggestion = log.getReviewResultJson().get("suggestion");
            if (suggestion instanceof String suggestionText && !suggestionText.isBlank()) {
                values.add((isZh(plan) ? "复盘建议：" : "Review suggestion: ") + suggestionText.trim());
            }
        });
        return values.stream().distinct().limit(6).toList();
    }

    private void saveAdaptiveScheduleOverride(LearningPlan plan, AdaptiveScheduleOverrideSummary override) {
        Map<String, Object> planJson = plan.getPlanJson() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(plan.getPlanJson());
        planJson.put("adaptiveScheduleOverride", objectMapper.convertValue(
                override,
                new TypeReference<Map<String, Object>>() {
                }
        ));
        plan.setPlanJson(planJson);
    }

    private String defaultOverrideReason(LearningPlan plan, String pacing) {
        if (isZh(plan)) {
            return switch (pacing) {
                case "lighter" -> "用户手动要求后续两天降低任务密度。";
                case "stronger" -> "用户手动要求后续两天提升任务密度。";
                default -> "用户手动要求后续两天保持当前任务密度。";
            };
        }
        return switch (pacing) {
            case "lighter" -> "The user manually requested a lighter load for the next two days.";
            case "stronger" -> "The user manually requested a stronger load for the next two days.";
            default -> "The user manually requested the current load to stay steady for the next two days.";
        };
    }

    private record ProgressSignal(
            double completionRate,
            int blockerCount,
            String paceAdjustment,
            String impact
    ) {
    }
}
