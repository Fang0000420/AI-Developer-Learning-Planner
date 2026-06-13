package com.aidevplanner.backend.profile;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.agent.AgentClientResponse;
import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.agent.AgentRunStatus;
import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.knowledge.KnowledgeContextBundle;
import com.aidevplanner.backend.knowledge.KnowledgeContextService;
import com.aidevplanner.backend.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProfileAnalysisService {

    static final String AGENT_NAME = "Profile Analyzer";

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;
    private final ProfileAnalyzerClient profileAnalyzerClient;
    private final SkillProfileRepository skillProfileRepository;
    private final KnowledgeContextService knowledgeContextService;
    private final UserProfileService userProfileService;

    public ProfileAnalysisService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            GoalRepository goalRepository,
            ObjectMapper objectMapper,
            ProfileAnalyzerClient profileAnalyzerClient,
            SkillProfileRepository skillProfileRepository,
            KnowledgeContextService knowledgeContextService,
            UserProfileService userProfileService
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.goalRepository = goalRepository;
        this.objectMapper = objectMapper;
        this.profileAnalyzerClient = profileAnalyzerClient;
        this.skillProfileRepository = skillProfileRepository;
        this.knowledgeContextService = knowledgeContextService;
        this.userProfileService = userProfileService;
    }

    @Transactional(readOnly = true)
    public SkillProfileResponse getLatestProfile(Long goalId) {
        ensureCanAccessGoal(goalId);

        return skillProfileRepository.findFirstByGoalIdOrderByCreatedAtDesc(goalId)
                .map(SkillProfileResponse::from)
                .orElse(null);
    }

    @Transactional(noRollbackFor = AgentServiceException.class)
    public SkillProfileResponse analyzeGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
        ProfileAnalyzeRequest request = buildRequest(goal);
        String inputJson = writeJson(request);
        long startedAt = System.nanoTime();

        try {
            AgentClientResponse<ProfileAnalyzeResponse> clientResponse = profileAnalyzerClient.analyze(request);
            ProfileAnalyzeResponse response = clientResponse.payload();
            long latencyMs = elapsedMs(startedAt);
            String outputJson = writeJson(response);
            SkillProfile profile = skillProfileRepository.save(toSkillProfile(goal, response));
            userProfileService.recordGoalAnalysis(goal, profile, response);

            agentRunRepository.save(new AgentRun(
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

            return SkillProfileResponse.from(profile);
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

    private ProfileAnalyzeRequest buildRequest(Goal goal) {
        User user = goal.getUser();
        String background = firstPresent(
                user.getBackground(),
                goal.getDescription(),
                "No technical background recorded."
        );
        BigDecimal dailyAvailableHours = user.getDailyAvailableHours() == null
                ? BigDecimal.ONE
                : user.getDailyAvailableHours();
        KnowledgeContextBundle knowledgeContext = knowledgeContextService.buildForGoal(goal);

        return new ProfileAnalyzeRequest(
                background,
                goal.getTitle(),
                dailyAvailableHours,
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

    private SkillProfile toSkillProfile(Goal goal, ProfileAnalyzeResponse response) {
        return new SkillProfile(
                goal.getUser(),
                goal,
                copyList(response.currentSkills()),
                copyList(response.strengths()),
                copyList(response.weaknesses()),
                firstPresent(response.recommendedDirection(), "No recommended direction returned.")
        );
    }

    private List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
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
