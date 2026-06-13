package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.cache.AsyncJobCacheService;
import com.aidevplanner.backend.common.ConcurrentJobExecutionException;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.idempotency.AsyncJobDeduplicationService;
import com.aidevplanner.backend.learningplan.LearningPlanGenerateRequest;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.learningplan.LearningPlanRepository;
import com.aidevplanner.backend.lock.ResourceLockHandle;
import com.aidevplanner.backend.lock.ResourceLockService;
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

    private static final String PATH_ANALYSIS_LOCK_PREFIX = "lock:path-analysis:goal:";
    private static final String PLAN_GENERATION_LOCK_PREFIX = "lock:plan-generation:goal:";
    private static final String PROGRESS_SUBMISSION_LOCK_PREFIX = "lock:progress-submission:plan:";
    private static final String PATH_ANALYSIS_ACTIVE_PREFIX = "active-job:path-analysis:goal:";
    private static final String PLAN_GENERATION_ACTIVE_PREFIX = "active-job:plan-generation:goal:";
    private static final String PROGRESS_SUBMISSION_ACTIVE_PREFIX = "active-job:progress-submission:plan:";

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobRunner asyncJobRunner;
    private final AsyncJobCacheService asyncJobCacheService;
    private final AsyncJobDeduplicationService asyncJobDeduplicationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalRepository goalRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ResourceLockService resourceLockService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public AsyncJobService(
            AsyncJobRepository asyncJobRepository,
            AsyncJobRunner asyncJobRunner,
            AsyncJobCacheService asyncJobCacheService,
            AsyncJobDeduplicationService asyncJobDeduplicationService,
            AuthenticatedUserService authenticatedUserService,
            GoalRepository goalRepository,
            LearningPlanRepository learningPlanRepository,
            ResourceLockService resourceLockService,
            ObjectMapper objectMapper,
            UserRepository userRepository
    ) {
        this.asyncJobRepository = asyncJobRepository;
        this.asyncJobRunner = asyncJobRunner;
        this.asyncJobCacheService = asyncJobCacheService;
        this.asyncJobDeduplicationService = asyncJobDeduplicationService;
        this.authenticatedUserService = authenticatedUserService;
        this.goalRepository = goalRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.resourceLockService = resourceLockService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public AsyncJobResponse createPlanGenerationJob(LearningPlanGenerateRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findById(request.goalId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", request.goalId()));
        ensureUserOwns(goal.getUser().getId(), request.goalId(), "Goal");
        String activeJobKey = PLAN_GENERATION_ACTIVE_PREFIX + request.goalId();
        AsyncJobResponse existingJob = activeJobResponse(activeJobKey);
        if (existingJob != null) {
            return existingJob;
        }
        ResourceLockHandle lockHandle = acquireLockOrFail(
                PLAN_GENERATION_LOCK_PREFIX + request.goalId(),
                activeJobKey,
                "A plan generation job is already running for this goal."
        );
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PLAN_GENERATION,
                writeJson(request),
                user
        ));
        AsyncJobResponse response = toResponse(job);
        asyncJobCacheService.put(response);
        asyncJobDeduplicationService.saveActiveJobId(activeJobKey, job.getId());
        asyncJobRunner.runPlanGeneration(
                job.getId(),
                request.goalId(),
                ObservabilityContext.getRequestId(),
                activeJobKey,
                lockHandle
        );
        return response;
    }

    public AsyncJobResponse createPathAnalysisJob(PathAnalysisRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findById(request.goalId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", request.goalId()));
        ensureUserOwns(goal.getUser().getId(), request.goalId(), "Goal");
        String activeJobKey = PATH_ANALYSIS_ACTIVE_PREFIX + request.goalId();
        AsyncJobResponse existingJob = activeJobResponse(activeJobKey);
        if (existingJob != null) {
            return existingJob;
        }
        ResourceLockHandle lockHandle = acquireLockOrFail(
                PATH_ANALYSIS_LOCK_PREFIX + request.goalId(),
                activeJobKey,
                "A path analysis job is already running for this goal."
        );
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PATH_ANALYSIS,
                writeJson(request),
                user
        ));
        AsyncJobResponse response = toResponse(job);
        asyncJobCacheService.put(response);
        asyncJobDeduplicationService.saveActiveJobId(activeJobKey, job.getId());
        asyncJobRunner.runPathAnalysis(
                job.getId(),
                request.goalId(),
                ObservabilityContext.getRequestId(),
                activeJobKey,
                lockHandle
        );
        return response;
    }

    public AsyncJobResponse createProgressSubmissionJob(ProgressSubmitRequest request) {
        User user = currentUser();
        LearningPlan plan = learningPlanRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan", request.planId()));
        ensureUserOwns(plan.getUser().getId(), request.planId(), "Learning plan");
        String activeJobKey = PROGRESS_SUBMISSION_ACTIVE_PREFIX + request.planId() + ":day:" + request.dayIndex();
        AsyncJobResponse existingJob = activeJobResponse(activeJobKey);
        if (existingJob != null) {
            return existingJob;
        }
        ResourceLockHandle lockHandle = acquireLockOrFail(
                PROGRESS_SUBMISSION_LOCK_PREFIX + request.planId() + ":day:" + request.dayIndex(),
                activeJobKey,
                "A progress submission job is already running for this plan day."
        );
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PROGRESS_SUBMISSION,
                writeJson(request),
                user
        ));
        AsyncJobResponse response = toResponse(job);
        asyncJobCacheService.put(response);
        asyncJobDeduplicationService.saveActiveJobId(activeJobKey, job.getId());
        asyncJobRunner.runProgressSubmission(
                job.getId(),
                request,
                ObservabilityContext.getRequestId(),
                activeJobKey,
                lockHandle
        );
        return response;
    }

    public AsyncJobResponse getJob(UUID jobId) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        if (currentUserId != null) {
            AsyncJobResponse cached = asyncJobCacheService.get(jobId).orElse(null);
            if (cached != null && ownsJob(jobId, currentUserId)) {
                return cached;
            }
        } else {
            AsyncJobResponse cached = asyncJobCacheService.get(jobId).orElse(null);
            if (cached != null) {
                return cached;
            }
        }
        AsyncJob job = currentUserId == null
                ? asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Async job", jobId.toString()))
                : asyncJobRepository.findByIdAndUserId(jobId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Async job", jobId.toString()));
        AsyncJobResponse response = toResponse(job);
        asyncJobCacheService.put(response);
        return response;
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

    private AsyncJobResponse activeJobResponse(String activeJobKey) {
        UUID activeJobId = asyncJobDeduplicationService.getActiveJobId(activeJobKey).orElse(null);
        if (activeJobId == null) {
            return null;
        }
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        AsyncJob job = currentUserId == null
                ? asyncJobRepository.findById(activeJobId).orElse(null)
                : asyncJobRepository.findByIdAndUserId(activeJobId, currentUserId).orElse(null);
        if (job == null) {
            asyncJobDeduplicationService.clear(activeJobKey);
            asyncJobCacheService.evict(activeJobId);
            return null;
        }
        if (job.getStatus() == AsyncJobStatus.PENDING || job.getStatus() == AsyncJobStatus.RUNNING) {
            AsyncJobResponse response = toResponse(job);
            asyncJobCacheService.put(response);
            return response;
        }
        asyncJobDeduplicationService.clear(activeJobKey);
        return null;
    }

    private ResourceLockHandle acquireLockOrFail(String lockKey, String activeJobKey, String message) {
        ResourceLockHandle lockHandle = resourceLockService.tryAcquire(lockKey);
        if (lockHandle != null) {
            return lockHandle;
        }
        AsyncJobResponse existingJob = activeJobResponse(activeJobKey);
        if (existingJob != null) {
            throw new ConcurrentJobExecutionException(message + " Job id: " + existingJob.jobId());
        }
        throw new ConcurrentJobExecutionException(message);
    }

    private boolean ownsJob(UUID jobId, Long currentUserId) {
        return asyncJobRepository.findByIdAndUserId(jobId, currentUserId).isPresent();
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
