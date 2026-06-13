package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.learningplan.LearningPlanGenerateRequest;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.learningplan.LearningPlanRepository;
import com.aidevplanner.backend.observability.ObservabilityContext;
import com.aidevplanner.backend.path.PathAnalysisRequest;
import com.aidevplanner.backend.progress.ProgressSubmitRequest;
import com.aidevplanner.backend.user.User;
import com.aidevplanner.backend.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AsyncJobService {

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobRunner asyncJobRunner;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalRepository goalRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public AsyncJobService(
            AsyncJobRepository asyncJobRepository,
            AsyncJobRunner asyncJobRunner,
            AuthenticatedUserService authenticatedUserService,
            GoalRepository goalRepository,
            LearningPlanRepository learningPlanRepository,
            ObjectMapper objectMapper,
            UserRepository userRepository
    ) {
        this.asyncJobRepository = asyncJobRepository;
        this.asyncJobRunner = asyncJobRunner;
        this.authenticatedUserService = authenticatedUserService;
        this.goalRepository = goalRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public AsyncJobResponse createPlanGenerationJob(LearningPlanGenerateRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findById(request.goalId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", request.goalId()));
        ensureUserOwns(goal.getUser().getId(), request.goalId(), "Goal");
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PLAN_GENERATION,
                writeJson(request),
                user
        ));
        asyncJobRunner.runPlanGeneration(job.getId(), request.goalId(), ObservabilityContext.getRequestId());
        return toResponse(job);
    }

    public AsyncJobResponse createPathAnalysisJob(PathAnalysisRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findById(request.goalId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", request.goalId()));
        ensureUserOwns(goal.getUser().getId(), request.goalId(), "Goal");
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PATH_ANALYSIS,
                writeJson(request),
                user
        ));
        asyncJobRunner.runPathAnalysis(job.getId(), request.goalId(), ObservabilityContext.getRequestId());
        return toResponse(job);
    }

    public AsyncJobResponse createProgressSubmissionJob(ProgressSubmitRequest request) {
        User user = currentUser();
        LearningPlan plan = learningPlanRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", request.planId()));
        ensureUserOwns(plan.getUser().getId(), request.planId(), "Learning plan");
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PROGRESS_SUBMISSION,
                writeJson(request),
                user
        ));
        asyncJobRunner.runProgressSubmission(job.getId(), request, ObservabilityContext.getRequestId());
        return toResponse(job);
    }

    public AsyncJobResponse getJob(UUID jobId) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        AsyncJob job = currentUserId == null
                ? asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Async job", jobId.toString()))
                : asyncJobRepository.findByIdAndUserId(jobId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Async job", jobId.toString()));
        return toResponse(job);
    }

    private User currentUser() {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        if (currentUserId == null) {
            return null;
        }
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
    }

    private void ensureUserOwns(Long ownerUserId, Object resourceId, String resourceName) {
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!ownerUserId.equals(currentUserId)) {
                throw new ResourceNotFoundException(resourceName, String.valueOf(resourceId));
            }
        });
    }

    private AsyncJobResponse toResponse(AsyncJob job) {
        return new AsyncJobResponse(
                job.getId(),
                job.getJobType(),
                job.getStatus(),
                readResult(job.getResultJson()),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private JsonNode readResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(resultJson);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode().put("raw", resultJson);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize async job input.", exception);
        }
    }
}
