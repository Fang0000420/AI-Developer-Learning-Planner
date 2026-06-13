package com.aidevplanner.backend.skillgap;

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
import com.aidevplanner.backend.knowledge.KnowledgeContextBundle;
import com.aidevplanner.backend.knowledge.KnowledgeContextService;
import com.aidevplanner.backend.profile.SkillProfile;
import com.aidevplanner.backend.profile.SkillProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SkillGapAnalysisService {

    static final String AGENT_NAME = "Skill Gap Analyzer";
    private static final String GOAL_DECOMPOSER_AGENT_NAME = "Goal Decomposer";

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;
    private final SkillGapAnalyzerClient skillGapAnalyzerClient;
    private final SkillProfileRepository skillProfileRepository;
    private final KnowledgeContextService knowledgeContextService;

    public SkillGapAnalysisService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            GoalRepository goalRepository,
            ObjectMapper objectMapper,
            SkillGapAnalyzerClient skillGapAnalyzerClient,
            SkillProfileRepository skillProfileRepository,
            KnowledgeContextService knowledgeContextService
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.goalRepository = goalRepository;
        this.objectMapper = objectMapper;
        this.skillGapAnalyzerClient = skillGapAnalyzerClient;
        this.skillProfileRepository = skillProfileRepository;
        this.knowledgeContextService = knowledgeContextService;
    }

    @Transactional(readOnly = true)
    public SkillGapAnalysisResponse getLatestSkillGapAnalysis(Long goalId) {
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
    public SkillGapAnalysisResponse analyzeSkillGap(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
        SkillGapAnalyzeRequest request = buildRequest(goal);
        String inputJson = writeJson(request);
        long startedAt = System.nanoTime();

        try {
            AgentClientResponse<SkillGapAnalyzeResponse> clientResponse = skillGapAnalyzerClient.analyze(request);
            SkillGapAnalyzeResponse response = normalizeResponse(clientResponse.payload());
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

    private SkillGapAnalyzeRequest buildRequest(Goal goal) {
        SkillProfile profile = skillProfileRepository
                .findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
                .orElse(null);
        KnowledgeContextBundle knowledgeContext = knowledgeContextService.buildForGoal(goal);

        return new SkillGapAnalyzeRequest(
                firstPresent(goal.getTitle(), "Untitled learning goal"),
                profile == null ? List.of() : copyList(profile.getCurrentSkills()),
                profile == null ? List.of() : copyList(profile.getStrengths()),
                profile == null ? List.of() : copyList(profile.getWeaknesses()),
                latestSubGoals(goal.getId()),
                knowledgeContext.contextText(),
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

    private SkillGapAnalysisResponse toResponse(AgentRun agentRun) {
        SkillGapAnalyzeResponse response = readOutput(agentRun.getOutputJson());
        Long goalId = agentRun.getGoal() == null ? null : agentRun.getGoal().getId();
        return new SkillGapAnalysisResponse(
                agentRun.getId(),
                goalId,
                response.skillGaps(),
                agentRun.getCreatedAt()
        );
    }

    private SkillGapAnalyzeResponse normalizeResponse(SkillGapAnalyzeResponse response) {
        List<SkillGapResponse> skillGaps = response.skillGaps() == null
                ? List.of()
                : response.skillGaps().stream()
                .filter(skillGap -> skillGap != null && !isBlank(skillGap.skill()))
                .map(skillGap -> new SkillGapResponse(
                        skillGap.skill().trim(),
                        firstPresent(skillGap.currentLevel(), "unknown"),
                        firstPresent(skillGap.targetLevel(), "intermediate"),
                        normalizePriority(skillGap.priority()),
                        firstPresent(skillGap.reason(), "No reason returned.")
                ))
                .sorted(Comparator.comparingInt(skillGap -> priorityRank(skillGap.priority())))
                .toList();

        if (skillGaps.size() < 4) {
            throw new AgentServiceException("Skill gap analyzer returned fewer than 4 skill gaps.");
        }

        return new SkillGapAnalyzeResponse(skillGaps);
    }

    private SkillGapAnalyzeResponse readOutput(String outputJson) {
        if (isBlank(outputJson)) {
            throw new AgentServiceException("Saved skill gap analysis output is empty.");
        }

        try {
            return normalizeResponse(objectMapper.readValue(outputJson, SkillGapAnalyzeResponse.class));
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Saved skill gap analysis output is invalid.", exception);
        }
    }

    private List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private String normalizePriority(String priority) {
        String normalized = firstPresent(priority, "medium").toLowerCase();
        return switch (normalized) {
            case "high", "medium", "low" -> normalized;
            default -> "medium";
        };
    }

    private int priorityRank(String priority) {
        return switch (priority) {
            case "high" -> 0;
            case "medium" -> 1;
            default -> 2;
        };
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
