package com.aidevplanner.backend.goaldecomposition;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoalDecompositionService {

    static final String AGENT_NAME = "Goal Decomposer";

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalDecomposerClient goalDecomposerClient;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;

    public GoalDecompositionService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            GoalDecomposerClient goalDecomposerClient,
            GoalRepository goalRepository,
            ObjectMapper objectMapper
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.goalDecomposerClient = goalDecomposerClient;
        this.goalRepository = goalRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GoalDecompositionResponse getLatestDecomposition(Long goalId) {
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
    public GoalDecompositionResponse decomposeGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
        GoalDecomposeRequest request = buildRequest(goal);
        String inputJson = writeJson(request);
        long startedAt = System.nanoTime();

        try {
            GoalDecomposeResponse response = normalizeResponse(goalDecomposerClient.decompose(request));
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

    private GoalDecomposeRequest buildRequest(Goal goal) {
        User user = goal.getUser();
        return new GoalDecomposeRequest(
                firstPresent(goal.getTitle(), "Untitled learning goal"),
                firstPresent(user.getBackground(), goal.getDescription(), null),
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

    private GoalDecompositionResponse toResponse(AgentRun agentRun) {
        GoalDecomposeResponse response = readOutput(agentRun.getOutputJson());
        Long goalId = agentRun.getGoal() == null ? null : agentRun.getGoal().getId();
        return new GoalDecompositionResponse(
                agentRun.getId(),
                goalId,
                response.subGoals(),
                agentRun.getCreatedAt()
        );
    }

    private GoalDecomposeResponse normalizeResponse(GoalDecomposeResponse response) {
        List<SubGoalResponse> subGoals = response.subGoals() == null
                ? List.of()
                : response.subGoals().stream()
                .filter(subGoal -> subGoal != null && !isBlank(subGoal.title()))
                .map(subGoal -> new SubGoalResponse(
                        subGoal.title().trim(),
                        firstPresent(subGoal.description(), "No description returned."),
                        normalizePriority(subGoal.priority())
                ))
                .toList();

        if (subGoals.isEmpty()) {
            throw new AgentServiceException("Goal decomposer returned no sub-goals.");
        }

        return new GoalDecomposeResponse(subGoals);
    }

    private GoalDecomposeResponse readOutput(String outputJson) {
        if (isBlank(outputJson)) {
            throw new AgentServiceException("Saved goal decomposition output is empty.");
        }

        try {
            return normalizeResponse(objectMapper.readValue(outputJson, GoalDecomposeResponse.class));
        } catch (JsonProcessingException exception) {
            throw new AgentServiceException("Saved goal decomposition output is invalid.", exception);
        }
    }

    private String normalizePriority(String priority) {
        String normalized = firstPresent(priority, "medium").toLowerCase();
        return switch (normalized) {
            case "high", "medium", "low" -> normalized;
            default -> "medium";
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
