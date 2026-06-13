package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.goal.ResponseLanguage;
import com.aidevplanner.backend.goaldecomposition.GoalDecomposeResponse;
import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;
import com.aidevplanner.backend.knowledge.KnowledgeBasisResponse;
import com.aidevplanner.backend.knowledge.KnowledgeContextBundle;
import com.aidevplanner.backend.knowledge.KnowledgeContextService;
import com.aidevplanner.backend.path.PathRecommendation;
import com.aidevplanner.backend.path.PathRecommendationRepository;
import com.aidevplanner.backend.profile.SkillProfile;
import com.aidevplanner.backend.profile.SkillProfileRepository;
import com.aidevplanner.backend.profile.UserProfile;
import com.aidevplanner.backend.profile.UserProfileRepository;
import com.aidevplanner.backend.progress.ProgressLog;
import com.aidevplanner.backend.progress.ProgressLogRepository;
import com.aidevplanner.backend.projectrecommendation.ProjectRecommendResponse;
import com.aidevplanner.backend.skillgap.SkillGapAnalyzeResponse;
import com.aidevplanner.backend.skillgap.SkillGapResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningPlanService {

    static final String AGENT_NAME = "Plan Generator";
    private static final String GOAL_DECOMPOSER_AGENT_NAME = "Goal Decomposer";
    private static final String PROJECT_RECOMMENDER_AGENT_NAME = "Project Recommender";
    private static final String SKILL_GAP_ANALYZER_AGENT_NAME = "Skill Gap Analyzer";

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final DailyTaskRepository dailyTaskRepository;
    private final GoalRepository goalRepository;
    private final KnowledgeContextService knowledgeContextService;
    private final LearningPlanRepository learningPlanRepository;
    private final LearningPlanVersionManager learningPlanVersionManager;
    private final ObjectMapper objectMapper;
    private final PlanGeneratorClient planGeneratorClient;
    private final PathRecommendationRepository pathRecommendationRepository;
    private final ProgressLogRepository progressLogRepository;
    private final SkillProfileRepository skillProfileRepository;
    private final UserProfileRepository userProfileRepository;

    public LearningPlanService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            DailyTaskRepository dailyTaskRepository,
            GoalRepository goalRepository,
            KnowledgeContextService knowledgeContextService,
            LearningPlanRepository learningPlanRepository,
            LearningPlanVersionManager learningPlanVersionManager,
            ObjectMapper objectMapper,
            PlanGeneratorClient planGeneratorClient,
            PathRecommendationRepository pathRecommendationRepository,
            ProgressLogRepository progressLogRepository,
            SkillProfileRepository skillProfileRepository,
            UserProfileRepository userProfileRepository
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.dailyTaskRepository = dailyTaskRepository;
        this.goalRepository = goalRepository;
        this.knowledgeContextService = knowledgeContextService;
        this.learningPlanRepository = learningPlanRepository;
        this.learningPlanVersionManager = learningPlanVersionManager;
        this.objectMapper = objectMapper;
        this.planGeneratorClient = planGeneratorClient;
        this.pathRecommendationRepository = pathRecommendationRepository;
        this.progressLogRepository = progressLogRepository;
        this.skillProfileRepository = skillProfileRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<LearningPlanSummaryResponse> listPlans() {
        List<LearningPlan> plans = authenticatedUserService.currentUserId()
                .map(learningPlanRepository::findByUserIdOrderByCreatedAtDesc)
                .orElseGet(learningPlanRepository::findAllByOrderByCreatedAtDesc);
        return plans.stream()
                .map(plan -> toSummaryResponse(
                        plan,
                        dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(plan.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningPlanResponse getPlan(Long planId) {
        LearningPlan plan = findPlan(planId);
        List<DailyTask> tasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId);
        return toResponse(plan, tasks);
    }

    @Transactional(readOnly = true)
    public List<PlanTaskResponse> listTasks(Long planId) {
        ensurePlanExists(planId);
        return dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanDayResponse getDayTasks(Long planId, Integer dayIndex) {
        ensurePlanExists(planId);
        List<DailyTask> tasks = dailyTaskRepository.findByPlanIdAndDayIndexOrderByTaskOrderAsc(planId, dayIndex);
        if (tasks.isEmpty()) {
            throw new ResourceNotFoundException("Learning plan day", dayIndex.longValue());
        }
        return toDayResponse(dayIndex, tasks);
    }

    @Transactional
    public LearningPlanResponse updatePlanStatus(Long planId, LearningPlanUpdateRequest request) {
        LearningPlan plan = findPlan(planId);
        plan.setStatus(request.status());
        List<DailyTask> tasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId);
        return toResponse(plan, tasks);
    }

    @Transactional
    public PlanTaskResponse updateTaskStatus(
            Long planId,
            Long taskId,
            DailyTaskStatusUpdateRequest request
    ) {
        ensurePlanExists(planId);
        DailyTask task = dailyTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily task", taskId));
        if (!task.getPlan().getId().equals(planId)) {
            throw new ResourceNotFoundException("Daily task in learning plan", taskId);
        }
        task.setStatus(request.status());
        return toTaskResponse(task);
    }

    @Transactional
    public void deletePlan(Long planId) {
        if (authenticatedUserService.currentUserId().isEmpty()) {
            if (!learningPlanRepository.existsById(planId)) {
                throw new ResourceNotFoundException("Learning plan", planId);
            }
            learningPlanRepository.deleteById(planId);
            return;
        }
        findPlan(planId);
        learningPlanRepository.deleteById(planId);
    }

    @Transactional
    public LearningPlanResponse restoreVersion(Long planId, Integer version) {
        LearningPlan plan = findPlan(planId);
        learningPlanVersionManager.restoreVersion(plan, version);
        List<DailyTask> tasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId);
        return toResponse(plan, tasks);
    }

    private void ensurePlanExists(Long planId) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        boolean exists = currentUserId == null
                ? learningPlanRepository.existsById(planId)
                : learningPlanRepository.existsByIdAndUserId(planId, currentUserId);
        if (!exists) {
            throw new ResourceNotFoundException("Learning plan", planId);
        }
    }

    private LearningPlan findPlan(Long planId) {
        LearningPlan plan = learningPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", planId));
        ensureCurrentUserOwns(plan);
        return plan;
    }

    @Transactional(noRollbackFor = AgentServiceException.class)
    public LearningPlanResponse generatePlan(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
        KnowledgeContextBundle knowledgeContext = knowledgeContextService.buildForGoal(goal);
        PlanGenerateAgentRequest request = buildRequest(goal, knowledgeContext);
        String inputJson = writeJson(request);
        long startedAt = System.nanoTime();

        try {
            AgentClientResponse<PlanGenerateAgentResponse> clientResponse = planGeneratorClient.generate(request);
            PlanGenerateAgentResponse agentResponse =
                    normalizeResponse(clientResponse.payload(), request);
            long latencyMs = elapsedMs(startedAt);
            String outputJson = writeJson(agentResponse);

            AgentRun savedRun = agentRunRepository.save(new AgentRun(
                    goal.getUser(),
                    goal,
                    AGENT_NAME,
                    inputJson,
                    outputJson,
                    AgentRunStatus.SUCCESS,
                    latencyMs,
                    clientResponse.responseSource(),
                    null
            ));

            LearningPlan savedPlan = learningPlanRepository.save(new LearningPlan(
                    goal.getUser(),
                    goal,
                    savedRun,
                    agentResponse.planTitle(),
                    agentResponse.durationDays(),
                    toPlanJson(agentResponse, knowledgeContextService.basisForGoal(goal, knowledgeContext.evidence()))
            ));
            savedRun.setPlan(savedPlan);
            List<DailyTask> savedTasks = dailyTaskRepository.saveAll(toDailyTasks(savedPlan, goal, agentResponse));
            learningPlanVersionManager.captureSnapshot(
                    savedPlan,
                    savedTasks,
                    "generated",
                    "Initial generated plan.",
                    savedTasks.stream().map(DailyTask::getDayIndex).distinct().toList()
            );

            return toResponse(savedPlan, savedTasks);
        } catch (AgentServiceException exception) {
            agentRunRepository.save(new AgentRun(
                    goal.getUser(),
                    goal,
                    AGENT_NAME,
                    inputJson,
                    null,
                    AgentRunStatus.FAILED,
                    elapsedMs(startedAt),
                    exception.getMessage()
            ));
            throw exception;
        }
    }

    private void ensureCurrentUserOwns(LearningPlan plan) {
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!plan.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Learning plan", plan.getId());
            }
        });
    }

    private void ensureCurrentUserOwns(Goal goal) {
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!goal.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Goal", goal.getId());
            }
        });
    }

    private PlanGenerateAgentRequest buildRequest(Goal goal, KnowledgeContextBundle knowledgeContext) {
        SkillProfile profile = skillProfileRepository
                .findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
                .orElse(null);
        UserProfile userProfile = userProfileRepository.findByUserId(goal.getUser().getId()).orElse(null);
        ProjectRecommendResponse projectRecommendation = latestProjectRecommendation(goal);
        PathRecommendation pathRecommendation = pathRecommendationRepository
                .findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
                .orElse(null);
        List<String> recentFeedback = latestFeedback(goal);
        List<String> planningConstraints = planningConstraints(goal, userProfile, pathRecommendation, recentFeedback);

        return new PlanGenerateAgentRequest(
                firstPresent(goal.getTitle(), "Untitled learning goal"),
                profile == null ? List.of() : copyList(profile.getCurrentSkills()),
                profile == null ? List.of() : copyList(profile.getStrengths()),
                profile == null ? List.of() : copyList(profile.getWeaknesses()),
                userProfile == null ? "" : firstPresent(userProfile.getProfileSummary()),
                userProfile == null ? "" : firstPresent(userProfile.getPacePreference()),
                userProfile == null ? "" : firstPresent(userProfile.getTimeBudgetNote()),
                userProfile == null ? "" : firstPresent(userProfile.getManualCorrection()),
                userProfile == null ? List.of() : cleanList(userProfile.getEvidence()),
                latestSubGoals(goal.getId()),
                latestSkillGaps(goal.getId()),
                pathRecommendation == null
                        ? firstPresent(projectRecommendation.recommendedProject(), fallbackTrackTitle(goal))
                        : firstPresent(pathRecommendation.getPathTitle(), projectRecommendation.recommendedProject(), fallbackTrackTitle(goal)),
                pathRecommendation == null
                        ? projectRecommendation.reason()
                        : firstPresent(pathRecommendation.getSummary(), projectRecommendation.reason()),
                pathRecommendation == null
                        ? projectRecommendation.difficulty()
                        : firstPresent(pathRecommendation.getDifficulty(), projectRecommendation.difficulty()),
                pathRecommendation == null
                        ? cleanList(projectRecommendation.coreTechStack())
                        : cleanList(pathRecommendation.getFocusAreas()),
                pathRecommendation == null
                        ? cleanList(projectRecommendation.finalDeliverables())
                        : cleanList(pathRecommendation.getFinalDeliverables()),
                planningConstraints,
                recentFeedback,
                knowledgeContext.contextText(),
                goal.getDurationDays(),
                goal.getUser().getDailyAvailableHours(),
                goal.getResponseLanguage().name()
        );
    }

    private List<String> latestFeedback(Goal goal) {
        return learningPlanRepository.findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
                .map(LearningPlan::getId)
                .map(progressLogRepository::findByPlanIdOrderByCreatedAtDesc)
                .stream()
                .flatMap(List::stream)
                .limit(2)
                .map(this::summarizeFeedback)
                .toList();
    }

    private String summarizeFeedback(ProgressLog progressLog) {
        Object suggestion = progressLog.getReviewResultJson().get("suggestion");
        String suggestionText = suggestion == null ? "" : String.valueOf(suggestion).trim();
        String feedback = firstPresent(progressLog.getUserFeedback());
        String blockers = cleanList(progressLog.getBlockers()).stream().limit(2).reduce((left, right) -> left + "；" + right).orElse("");
        return firstPresent(
                feedback,
                suggestionText,
                blockers,
                "Recent feedback captured in progress review."
        );
    }

    private List<String> planningConstraints(
            Goal goal,
            UserProfile userProfile,
            PathRecommendation pathRecommendation,
            List<String> recentFeedback
    ) {
        List<String> values = new ArrayList<>();
        if (userProfile != null) {
            if (!firstPresent(userProfile.getPacePreference()).isBlank()) {
                values.add(firstPresent(userProfile.getPacePreference()));
            }
            if (!firstPresent(userProfile.getTimeBudgetNote()).isBlank()) {
                values.add(firstPresent(userProfile.getTimeBudgetNote()));
            }
            if (!firstPresent(userProfile.getManualCorrection()).isBlank()) {
                values.add(isZh(goal)
                        ? "用户纠偏：" + userProfile.getManualCorrection().trim()
                        : "User correction: " + userProfile.getManualCorrection().trim());
            }
            values.addAll(cleanList(userProfile.getRiskSignals()).stream().limit(2).toList());
        }
        if (pathRecommendation != null) {
            values.addAll(cleanList(pathRecommendation.getRiskSignals()).stream().limit(2).toList());
        }
        values.addAll(recentFeedback.stream().limit(2).toList());
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private boolean isZh(Goal goal) {
        return goal.getResponseLanguage() == ResponseLanguage.ZH;
    }

    private ProjectRecommendResponse latestProjectRecommendation(Goal goal) {
        return agentRunRepository
                .findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                        goal.getId(),
                        PROJECT_RECOMMENDER_AGENT_NAME,
                        AgentRunStatus.SUCCESS
                )
                .map(AgentRun::getOutputJson)
                .map(this::readProjectRecommendationOutput)
                .orElseGet(() -> fallbackProjectRecommendation(goal));
    }

    private ProjectRecommendResponse readProjectRecommendationOutput(String outputJson) {
        if (isBlank(outputJson)) {
            return fallbackProjectRecommendation(null);
        }

        try {
            ProjectRecommendResponse response = objectMapper.readValue(outputJson, ProjectRecommendResponse.class);
            return new ProjectRecommendResponse(
                    firstPresent(response.recommendedProject(), fallbackTrackTitle(null)),
                    firstPresent(response.reason(), "Use a focused learning track with visible progress."),
                    firstPresent(response.difficulty(), "medium"),
                    response.durationDays(),
                    response.dailyTimeHours(),
                    cleanList(response.coreTechStack()),
                    cleanList(response.finalDeliverables())
            );
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Saved project recommendation output is invalid.", exception);
        }
    }

    private ProjectRecommendResponse fallbackProjectRecommendation(Goal goal) {
        boolean zh = goal != null && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
        return new ProjectRecommendResponse(
                fallbackTrackTitle(goal),
                zh
                        ? "用一条聚焦的学习主线把练习、应用和成果验证串起来。"
                        : "Use a focused learning track to connect practice, application, and visible progress.",
                zh ? "中等" : "medium",
                goal == null ? 21 : goal.getDurationDays(),
                goal == null ? BigDecimal.valueOf(2) : goal.getUser().getDailyAvailableHours(),
                zh
                        ? List.of("关键基础", "稳定练习", "场景应用", "反馈复盘")
                        : List.of("Core foundation", "Consistent practice", "Applied scenarios", "Feedback review"),
                zh
                        ? List.of("阶段性成果记录", "可展示的练习输出", "学习计划总览")
                        : List.of("Stage progress notes", "Visible practice outputs", "Learning plan overview")
        );
    }

    private List<SubGoalResponse> latestSubGoals(Long goalId) {
        return agentRunRepository
                .findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                        goalId,
                        GOAL_DECOMPOSER_AGENT_NAME,
                        AgentRunStatus.SUCCESS
                )
                .map(AgentRun::getOutputJson)
                .map(this::readGoalDecompositionOutput)
                .orElse(List.of());
    }

    private List<SubGoalResponse> readGoalDecompositionOutput(String outputJson) {
        if (isBlank(outputJson)) {
            return List.of();
        }

        try {
            GoalDecomposeResponse response = objectMapper.readValue(outputJson, GoalDecomposeResponse.class);
            return response.subGoals() == null ? List.of() : response.subGoals();
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Saved goal decomposition output is invalid.", exception);
        }
    }

    private List<SkillGapResponse> latestSkillGaps(Long goalId) {
        return agentRunRepository
                .findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                        goalId,
                        SKILL_GAP_ANALYZER_AGENT_NAME,
                        AgentRunStatus.SUCCESS
                )
                .map(AgentRun::getOutputJson)
                .map(this::readSkillGapOutput)
                .orElse(List.of());
    }

    private List<SkillGapResponse> readSkillGapOutput(String outputJson) {
        if (isBlank(outputJson)) {
            return List.of();
        }

        try {
            SkillGapAnalyzeResponse response = objectMapper.readValue(outputJson, SkillGapAnalyzeResponse.class);
            return response.skillGaps() == null ? List.of() : response.skillGaps();
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Saved skill gap analysis output is invalid.", exception);
        }
    }

    private PlanGenerateAgentResponse normalizeResponse(
            PlanGenerateAgentResponse response,
            PlanGenerateAgentRequest request
    ) {
        Integer durationDays = request.durationDays();
        List<PlanDayAgentResponse> days = new ArrayList<>();
        if (response.days() != null) {
            for (int index = 0; index < Math.min(response.days().size(), durationDays); index++) {
                days.add(normalizeDay(response.days().get(index), index + 1, request));
            }
        }
        while (days.size() < durationDays) {
            days.add(fallbackDay(days.size() + 1, request));
        }

        return new PlanGenerateAgentResponse(
                firstPresent(
                        response.planTitle(),
                        isZh(request)
                                ? durationDays + " 天 " + firstPresent(request.recommendedProject(), request.mainGoal()) + " 学习计划"
                                : durationDays + "-Day " + firstPresent(request.recommendedProject(), request.mainGoal()) + " Learning Plan"
                ),
                durationDays,
                days
        );
    }

    private PlanDayAgentResponse normalizeDay(
            PlanDayAgentResponse day,
            int fallbackDayIndex,
            PlanGenerateAgentRequest request
    ) {
        Integer dayIndex = positiveOrDefault(day.dayIndex(), fallbackDayIndex);
        List<PlanTaskAgentResponse> tasks = new ArrayList<>();
        if (day.tasks() != null) {
            for (PlanTaskAgentResponse task : day.tasks()) {
                tasks.add(normalizeTask(task, request));
            }
        }
        if (tasks.isEmpty()) {
            tasks = fallbackDay(dayIndex, request).tasks();
        }

        return new PlanDayAgentResponse(
                dayIndex,
                firstPresent(
                        day.theme(),
                        isZh(request) ? "第 " + dayIndex + " 天学习安排" : "Day " + dayIndex + " learning plan"
                ),
                tasks
        );
    }

    private PlanTaskAgentResponse normalizeTask(PlanTaskAgentResponse task, PlanGenerateAgentRequest request) {
        boolean zh = isZh(request);
        return new PlanTaskAgentResponse(
                firstPresent(task.title(), zh ? "聚焦规划任务" : "Focused planner task"),
                firstPresent(
                        task.description(),
                        zh
                                ? "完成计划中的学习、练习或应用任务。"
                                : "Complete the planned learning, practice, or application work."
                ),
                positiveOrDefault(task.estimatedMinutes(), 45),
                firstPresent(task.type(), "practice"),
                firstPresent(task.deliverable(), zh ? "阶段性学习产物" : "Progress artifact"),
                normalizePriority(task.priority())
        );
    }

    private PlanDayAgentResponse fallbackDay(int dayIndex, PlanGenerateAgentRequest request) {
        boolean zh = isZh(request);
        String focus = zh ? "学习重点" : "learning focus";
        if (request.subGoals() != null && !request.subGoals().isEmpty()) {
            focus = request.subGoals().get((dayIndex - 1) % request.subGoals().size()).title();
        } else if (request.skillGaps() != null && !request.skillGaps().isEmpty()) {
            focus = request.skillGaps().get((dayIndex - 1) % request.skillGaps().size()).skill();
        }

        return new PlanDayAgentResponse(
                dayIndex,
                zh ? "围绕 " + focus + " 学习与验证" : "Learn and validate " + focus,
                List.of(
                        new PlanTaskAgentResponse(
                                zh ? "梳理第 " + dayIndex + " 天学习重点" : "Map Day " + dayIndex + " learning focus",
                                zh
                                        ? "复盘相关目标上下文，并确定今天最关键的学习任务。"
                                        : "Review the relevant goal context and decide the most important learning task.",
                                30,
                                "learn",
                                zh ? "第 " + dayIndex + " 天学习重点说明" : "Day " + dayIndex + " focus notes",
                                "high"
                        ),
                        new PlanTaskAgentResponse(
                                zh ? "练习并应用 " + focus : "Practice and apply " + focus,
                                zh
                                        ? "完成一个可验证的小练习或实际应用任务。"
                                        : "Complete a small but verifiable exercise or applied task.",
                                75,
                                "practice",
                                zh ? focus + " 阶段成果" : focus + " progress artifact",
                                "high"
                        ),
                        new PlanTaskAgentResponse(
                                zh ? "验证第 " + dayIndex + " 天进展" : "Verify Day " + dayIndex + " progress",
                                zh
                                        ? "回看今天结果，并记录下一步要继续的内容。"
                                        : "Review today's results and record what should continue next.",
                                15,
                                "review",
                                zh
                                        ? "第 " + dayIndex + " 天验证记录"
                                        : "Day " + dayIndex + " verification note",
                                "medium"
                        )
                )
        );
    }

    private List<DailyTask> toDailyTasks(
            LearningPlan plan,
            Goal goal,
            PlanGenerateAgentResponse response
    ) {
        List<DailyTask> tasks = new ArrayList<>();
        for (PlanDayAgentResponse day : response.days()) {
            List<PlanTaskAgentResponse> dayTasks = day.tasks() == null ? List.of() : day.tasks();
            for (int index = 0; index < dayTasks.size(); index++) {
                PlanTaskAgentResponse task = dayTasks.get(index);
                tasks.add(new DailyTask(
                        plan,
                        goal.getUser(),
                        goal,
                        day.dayIndex(),
                        index + 1,
                        day.theme(),
                        task.title(),
                        task.description(),
                        task.estimatedMinutes(),
                        task.type(),
                        task.deliverable(),
                        task.priority()
                ));
            }
        }
        return tasks;
    }

    private LearningPlanResponse toResponse(LearningPlan plan, List<DailyTask> tasks) {
        Map<Integer, List<DailyTask>> byDay = new LinkedHashMap<>();
        for (DailyTask task : tasks) {
            byDay.computeIfAbsent(task.getDayIndex(), ignored -> new ArrayList<>()).add(task);
        }

        List<PlanDayResponse> days = byDay.entrySet().stream()
                .map(entry -> toDayResponse(entry.getKey(), entry.getValue()))
                .toList();

        Long sourceAgentRunId = plan.getSourceAgentRun() == null ? null : plan.getSourceAgentRun().getId();
        KnowledgeBasisResponse knowledgeBasis = readKnowledgeBasis(plan);
        return new LearningPlanResponse(
                plan.getId(),
                plan.getGoal().getId(),
                plan.getUser().getId(),
                sourceAgentRunId,
                plan.getPlanTitle(),
                plan.getDurationDays(),
                plan.getStatus(),
                days,
                learningPlanVersionManager.versions(plan),
                knowledgeBasis,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private LearningPlanSummaryResponse toSummaryResponse(LearningPlan plan, List<DailyTask> tasks) {
        int dayCount = (int) tasks.stream()
                .map(DailyTask::getDayIndex)
                .distinct()
                .count();
        int totalEstimatedMinutes = tasks.stream()
                .mapToInt(DailyTask::getEstimatedMinutes)
                .sum();
        Long goalId = plan.getGoal() == null ? null : plan.getGoal().getId();
        Long userId = plan.getUser() == null ? null : plan.getUser().getId();

        return new LearningPlanSummaryResponse(
                plan.getId(),
                goalId,
                userId,
                plan.getPlanTitle(),
                plan.getDurationDays(),
                plan.getStatus(),
                dayCount,
                tasks.size(),
                totalEstimatedMinutes,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private PlanDayResponse toDayResponse(Integer dayIndex, List<DailyTask> tasks) {
        String theme = tasks.isEmpty() ? "" : tasks.get(0).getDayTheme();
        List<PlanTaskResponse> taskResponses = tasks.stream()
                .map(this::toTaskResponse)
                .toList();
        int totalEstimatedMinutes = taskResponses.stream()
                .mapToInt(PlanTaskResponse::estimatedMinutes)
                .sum();

        return new PlanDayResponse(dayIndex, theme, totalEstimatedMinutes, taskResponses);
    }

    private PlanTaskResponse toTaskResponse(DailyTask task) {
        return new PlanTaskResponse(
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

    private Map<String, Object> toPlanJson(
            PlanGenerateAgentResponse response,
            KnowledgeBasisResponse knowledgeBasis
    ) {
        Map<String, Object> value = objectMapper.convertValue(response, new TypeReference<>() {
        });
        value.put("knowledgeBasis", objectMapper.convertValue(knowledgeBasis, new TypeReference<Map<String, Object>>() {
        }));
        return value;
    }

    private KnowledgeBasisResponse readKnowledgeBasis(LearningPlan plan) {
        Object value = plan.getPlanJson().get("knowledgeBasis");
        if (value != null) {
            return objectMapper.convertValue(value, KnowledgeBasisResponse.class);
        }
        return knowledgeContextService.basisForGoal(plan.getGoal(), List.of());
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

    private List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String fallbackTrackTitle(Goal goal) {
        boolean zh = goal != null && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
        String title = goal == null ? "" : firstPresent(goal.getTitle());
        if (!title.isBlank()) {
            return zh ? title + " 学习主线" : title + " learning track";
        }
        return zh ? "通用学习主线" : "General learning track";
    }

    private boolean isZh(PlanGenerateAgentRequest request) {
        return "zh".equalsIgnoreCase(request.responseLanguage());
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Unable to serialize agent run payload.", exception);
        }
    }

    private long elapsedMs(long startedAt) {
        return Math.max(1, (System.nanoTime() - startedAt) / 1_000_000);
    }
}
