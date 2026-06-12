package com.aidevplanner.backend.projectrecommendation;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
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
import com.aidevplanner.backend.skillgap.SkillGapAnalyzeResponse;
import com.aidevplanner.backend.skillgap.SkillGapResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProjectRecommendationService {

    static final String AGENT_NAME = "Project Recommender";
    private static final String GOAL_DECOMPOSER_AGENT_NAME = "Goal Decomposer";
    private static final String SKILL_GAP_ANALYZER_AGENT_NAME = "Skill Gap Analyzer";

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;
    private final ProjectRecommenderClient projectRecommenderClient;
    private final SkillProfileRepository skillProfileRepository;

    public ProjectRecommendationService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            GoalRepository goalRepository,
            ObjectMapper objectMapper,
            ProjectRecommenderClient projectRecommenderClient,
            SkillProfileRepository skillProfileRepository
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.goalRepository = goalRepository;
        this.objectMapper = objectMapper;
        this.projectRecommenderClient = projectRecommenderClient;
        this.skillProfileRepository = skillProfileRepository;
    }

    @Transactional(readOnly = true)
    public ProjectRecommendationResponse getLatestProjectRecommendation(Long goalId) {
        ensureCanAccessGoal(goalId);

        return agentRunRepository
                .findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
                        goalId,
                        AGENT_NAME,
                        AgentRunStatus.SUCCESS
                )
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(noRollbackFor = AgentServiceException.class)
    public ProjectRecommendationResponse recommendProject(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
        ProjectRecommendRequest request = buildRequest(goal);
        String inputJson = writeJson(request);
        long startedAt = System.nanoTime();

        try {
            AgentClientResponse<ProjectRecommendResponse> clientResponse = projectRecommenderClient.recommend(request);
            ProjectRecommendResponse response = normalizeResponse(clientResponse.payload(), goal);
            long latencyMs = elapsedMs(startedAt);
            String outputJson = writeJson(response);

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

            return toResponse(savedRun);
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

    private ProjectRecommendRequest buildRequest(Goal goal) {
        SkillProfile profile = skillProfileRepository
                .findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
                .orElse(null);

        return new ProjectRecommendRequest(
                firstPresent(goal.getTitle(), "Untitled learning goal"),
                profile == null ? List.of() : copyList(profile.getCurrentSkills()),
                profile == null ? List.of() : copyList(profile.getStrengths()),
                profile == null ? List.of() : copyList(profile.getWeaknesses()),
                latestSubGoals(goal.getId()),
                latestSkillGaps(goal.getId()),
                goal.getDurationDays(),
                goal.getUser().getDailyAvailableHours(),
                goal.getResponseLanguage().name()
        );
    }

    private void ensureCanAccessGoal(Long goalId) {
        if (authenticatedUserService.currentUserId().isEmpty()) {
            if (!goalRepository.existsById(goalId)) {
                throw new ResourceNotFoundException("Goal", goalId);
            }
            return;
        }
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
    }

    private void ensureCurrentUserOwns(Goal goal) {
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!goal.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Goal", goal.getId());
            }
        });
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

    private ProjectRecommendationResponse toResponse(AgentRun agentRun) {
        ProjectRecommendResponse response = readOutput(agentRun.getOutputJson(), agentRun.getGoal());
        Long goalId = agentRun.getGoal() == null ? null : agentRun.getGoal().getId();
        return new ProjectRecommendationResponse(
                agentRun.getId(),
                goalId,
                response.recommendedProject(),
                response.reason(),
                response.difficulty(),
                response.durationDays(),
                response.dailyTimeHours(),
                response.coreTechStack(),
                response.finalDeliverables(),
                agentRun.getCreatedAt()
        );
    }

    private ProjectRecommendResponse normalizeResponse(ProjectRecommendResponse response, Goal goal) {
        boolean zh = goal != null && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
        Integer durationDays = response.durationDays();
        if (durationDays == null || durationDays <= 0) {
            durationDays = goal == null ? 21 : goal.getDurationDays();
        }

        BigDecimal dailyTimeHours = response.dailyTimeHours();
        if (dailyTimeHours == null || dailyTimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            dailyTimeHours = goal == null ? BigDecimal.valueOf(2) : goal.getUser().getDailyAvailableHours();
        }
        if (dailyTimeHours == null || dailyTimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            dailyTimeHours = BigDecimal.valueOf(2);
        }

        List<String> coreTechStack = cleanList(response.coreTechStack());
        if (coreTechStack.isEmpty()) {
            coreTechStack = defaultFocusAreas(zh);
        }

        List<String> finalDeliverables = cleanList(response.finalDeliverables());
        if (finalDeliverables.isEmpty()) {
            finalDeliverables = defaultExpectedOutcomes(zh);
        }

        return new ProjectRecommendResponse(
                firstPresent(
                        response.recommendedProject(),
                        fallbackTrackTitle(goal, zh)
                ),
                firstPresent(response.reason(), zh ? "未返回推荐理由。" : "No recommendation reason returned."),
                firstPresent(response.difficulty(), zh ? "中等" : "medium"),
                durationDays,
                dailyTimeHours,
                coreTechStack,
                finalDeliverables
        );
    }

    private ProjectRecommendResponse readOutput(String outputJson, Goal goal) {
        if (isBlank(outputJson)) {
            throw new AgentServiceException("Saved project recommendation output is empty.");
        }

        try {
            return normalizeResponse(objectMapper.readValue(outputJson, ProjectRecommendResponse.class), goal);
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Saved project recommendation output is invalid.", exception);
        }
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

    private String fallbackTrackTitle(Goal goal, boolean zh) {
        String goalTitle = goal == null ? "" : firstPresent(goal.getTitle());
        if (!goalTitle.isBlank()) {
            return zh ? goalTitle + " 学习主线" : goalTitle + " learning track";
        }
        return zh ? "通用学习主线" : "General learning track";
    }

    private List<String> defaultFocusAreas(boolean zh) {
        return zh
                ? List.of("关键基础", "稳定练习", "场景应用", "反馈复盘")
                : List.of("Core foundation", "Consistent practice", "Applied scenarios", "Feedback review");
    }

    private List<String> defaultExpectedOutcomes(boolean zh) {
        return zh
                ? List.of("阶段性成果记录", "可展示的练习输出", "复盘笔记")
                : List.of("Stage progress notes", "Visible practice outputs", "Review summary");
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
