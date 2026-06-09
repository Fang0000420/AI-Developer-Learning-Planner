package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.goaldecomposition.GoalDecomposeResponse;
import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;
import com.aidevplanner.backend.profile.SkillProfile;
import com.aidevplanner.backend.profile.SkillProfileRepository;
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
    private final DailyTaskRepository dailyTaskRepository;
    private final GoalRepository goalRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;
    private final PlanGeneratorClient planGeneratorClient;
    private final SkillProfileRepository skillProfileRepository;

    public LearningPlanService(
            AgentRunRepository agentRunRepository,
            DailyTaskRepository dailyTaskRepository,
            GoalRepository goalRepository,
            LearningPlanRepository learningPlanRepository,
            ObjectMapper objectMapper,
            PlanGeneratorClient planGeneratorClient,
            SkillProfileRepository skillProfileRepository
    ) {
        this.agentRunRepository = agentRunRepository;
        this.dailyTaskRepository = dailyTaskRepository;
        this.goalRepository = goalRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.objectMapper = objectMapper;
        this.planGeneratorClient = planGeneratorClient;
        this.skillProfileRepository = skillProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<LearningPlanSummaryResponse> listPlans() {
        return learningPlanRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(plan -> toSummaryResponse(
                        plan,
                        dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(plan.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningPlanResponse getPlan(Long planId) {
        LearningPlan plan = learningPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", planId));
        List<DailyTask> tasks = dailyTaskRepository.findByPlanIdOrderByDayIndexAscTaskOrderAsc(planId);
        return toResponse(plan, tasks);
    }

    @Transactional(noRollbackFor = AgentServiceException.class)
    public LearningPlanResponse generatePlan(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        PlanGenerateAgentRequest request = buildRequest(goal);
        String inputJson = writeJson(request);
        long startedAt = System.nanoTime();

        try {
            PlanGenerateAgentResponse agentResponse =
                    normalizeResponse(planGeneratorClient.generate(request), request);
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
                    null
            ));

            LearningPlan savedPlan = learningPlanRepository.save(new LearningPlan(
                    goal.getUser(),
                    goal,
                    savedRun,
                    agentResponse.planTitle(),
                    agentResponse.durationDays(),
                    toPlanJson(agentResponse)
            ));
            List<DailyTask> savedTasks = dailyTaskRepository.saveAll(toDailyTasks(savedPlan, goal, agentResponse));

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

    private PlanGenerateAgentRequest buildRequest(Goal goal) {
        SkillProfile profile = skillProfileRepository
                .findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
                .orElse(null);
        ProjectRecommendResponse projectRecommendation = latestProjectRecommendation(goal);

        return new PlanGenerateAgentRequest(
                firstPresent(goal.getTitle(), "Untitled learning goal"),
                profile == null ? List.of() : copyList(profile.getCurrentSkills()),
                profile == null ? List.of() : copyList(profile.getStrengths()),
                profile == null ? List.of() : copyList(profile.getWeaknesses()),
                latestSubGoals(goal.getId()),
                latestSkillGaps(goal.getId()),
                firstPresent(projectRecommendation.recommendedProject(), "AI Developer Learning Planner"),
                projectRecommendation.reason(),
                projectRecommendation.difficulty(),
                cleanList(projectRecommendation.coreTechStack()),
                cleanList(projectRecommendation.finalDeliverables()),
                goal.getDurationDays(),
                goal.getUser().getDailyAvailableHours()
        );
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
                    firstPresent(response.recommendedProject(), "AI Developer Learning Planner"),
                    firstPresent(response.reason(), "Build the planner MVP as the learning project."),
                    firstPresent(response.difficulty(), "medium-high"),
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
        return new ProjectRecommendResponse(
                "AI Developer Learning Planner",
                "Build the planner MVP as the learning project.",
                "medium-high",
                goal == null ? 21 : goal.getDurationDays(),
                goal == null ? BigDecimal.valueOf(2) : goal.getUser().getDailyAvailableHours(),
                List.of("Spring Boot", "FastAPI", "DeepSeek", "PostgreSQL", "Next.js"),
                List.of("Runnable full-stack demo", "Agent run records", "Learning plan overview")
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
                        durationDays + "-Day " + request.recommendedProject() + " Build Plan"
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
                tasks.add(normalizeTask(task));
            }
        }
        if (tasks.isEmpty()) {
            tasks = fallbackDay(dayIndex, request).tasks();
        }

        return new PlanDayAgentResponse(
                dayIndex,
                firstPresent(day.theme(), "Day " + dayIndex + " implementation"),
                tasks
        );
    }

    private PlanTaskAgentResponse normalizeTask(PlanTaskAgentResponse task) {
        return new PlanTaskAgentResponse(
                firstPresent(task.title(), "Focused planner task"),
                firstPresent(task.description(), "Complete the planned learning and implementation work."),
                positiveOrDefault(task.estimatedMinutes(), 45),
                firstPresent(task.type(), "build"),
                firstPresent(task.deliverable(), "Working progress artifact"),
                normalizePriority(task.priority())
        );
    }

    private PlanDayAgentResponse fallbackDay(int dayIndex, PlanGenerateAgentRequest request) {
        String focus = "planner MVP capability";
        if (request.subGoals() != null && !request.subGoals().isEmpty()) {
            focus = request.subGoals().get((dayIndex - 1) % request.subGoals().size()).title();
        } else if (request.skillGaps() != null && !request.skillGaps().isEmpty()) {
            focus = request.skillGaps().get((dayIndex - 1) % request.skillGaps().size()).skill();
        }

        return new PlanDayAgentResponse(
                dayIndex,
                "Build and verify " + focus,
                List.of(
                        new PlanTaskAgentResponse(
                                "Clarify Day " + dayIndex + " scope",
                                "Review the relevant goal context and decide the smallest useful slice.",
                                30,
                                "learning",
                                "Day " + dayIndex + " scope notes",
                                "high"
                        ),
                        new PlanTaskAgentResponse(
                                "Implement " + focus,
                                "Build the planned slice and connect it to the MVP workflow.",
                                75,
                                "build",
                                "Working " + focus + " slice",
                                "high"
                        ),
                        new PlanTaskAgentResponse(
                                "Verify Day " + dayIndex + " progress",
                                "Run focused checks and record what should continue next.",
                                15,
                                "review",
                                "Day " + dayIndex + " verification note",
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
        return new LearningPlanResponse(
                plan.getId(),
                plan.getGoal().getId(),
                plan.getUser().getId(),
                sourceAgentRunId,
                plan.getPlanTitle(),
                plan.getDurationDays(),
                days,
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

    private Map<String, Object> toPlanJson(PlanGenerateAgentResponse response) {
        return objectMapper.convertValue(response, new TypeReference<>() {
        });
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
