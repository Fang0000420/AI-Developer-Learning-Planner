package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.cache.AsyncJobCacheService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.idempotency.AsyncJobDeduplicationService;
import com.aidevplanner.backend.learningplan.LearningPlanResponse;
import com.aidevplanner.backend.learningplan.LearningPlanService;
import com.aidevplanner.backend.lock.ResourceLockHandle;
import com.aidevplanner.backend.lock.ResourceLockService;
import com.aidevplanner.backend.observability.ObservabilityContext;
import com.aidevplanner.backend.path.PathRecommendationResponse;
import com.aidevplanner.backend.path.PathRecommendationService;
import com.aidevplanner.backend.progress.ProgressLogResponse;
import com.aidevplanner.backend.progress.ProgressLogService;
import com.aidevplanner.backend.progress.ProgressSubmitRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AsyncJobRunner {

    private final AsyncJobCacheService asyncJobCacheService;
    private final AsyncJobDeduplicationService asyncJobDeduplicationService;
    private final AsyncJobRepository asyncJobRepository;
    private final LearningPlanService learningPlanService;
    private final ResourceLockService resourceLockService;
    private final ObjectMapper objectMapper;
    private final PathRecommendationService pathRecommendationService;
    private final ProgressLogService progressLogService;

    public AsyncJobRunner(
            AsyncJobCacheService asyncJobCacheService,
            AsyncJobDeduplicationService asyncJobDeduplicationService,
            AsyncJobRepository asyncJobRepository,
            LearningPlanService learningPlanService,
            ResourceLockService resourceLockService,
            ObjectMapper objectMapper,
            PathRecommendationService pathRecommendationService,
            ProgressLogService progressLogService
    ) {
        this.asyncJobCacheService = asyncJobCacheService;
        this.asyncJobDeduplicationService = asyncJobDeduplicationService;
        this.asyncJobRepository = asyncJobRepository;
        this.learningPlanService = learningPlanService;
        this.resourceLockService = resourceLockService;
        this.objectMapper = objectMapper;
        this.pathRecommendationService = pathRecommendationService;
        this.progressLogService = progressLogService;
    }

    @Async
    public void runPathAnalysis(
            UUID jobId,
            Long goalId,
            String requestId,
            String activeJobKey,
            ResourceLockHandle lockHandle
    ) {
        ObservabilityContext.setRequestId(requestId);
        try {
            markRunning(jobId);
            PathRecommendationResponse response = pathRecommendationService.analyzeGoal(goalId);
            markSucceeded(jobId, writeJson(response));
        } catch (Exception exception) {
            markFailed(jobId, exception);
        } finally {
            asyncJobDeduplicationService.clear(activeJobKey);
            resourceLockService.release(lockHandle);
            ObservabilityContext.clear();
        }
    }

    @Async
    public void runPlanGeneration(
            UUID jobId,
            Long goalId,
            String requestId,
            String activeJobKey,
            ResourceLockHandle lockHandle
    ) {
        ObservabilityContext.setRequestId(requestId);
        try {
            markRunning(jobId);
            LearningPlanResponse response = learningPlanService.generatePlan(goalId);
            markSucceeded(jobId, writeJson(response));
        } catch (Exception exception) {
            markFailed(jobId, exception);
        } finally {
            asyncJobDeduplicationService.clear(activeJobKey);
            resourceLockService.release(lockHandle);
            ObservabilityContext.clear();
        }
    }

    @Async
    public void runProgressSubmission(
            UUID jobId,
            ProgressSubmitRequest request,
            String requestId,
            String activeJobKey,
            ResourceLockHandle lockHandle
    ) {
        ObservabilityContext.setRequestId(requestId);
        try {
            markRunning(jobId);
            ProgressLogResponse response = progressLogService.submitProgress(request);
            markSucceeded(jobId, writeJson(response));
        } catch (Exception exception) {
            markFailed(jobId, exception);
        } finally {
            asyncJobDeduplicationService.clear(activeJobKey);
            resourceLockService.release(lockHandle);
            ObservabilityContext.clear();
        }
    }

    private void markRunning(UUID jobId) {
        AsyncJob job = getJob(jobId);
        job.markRunning();
        asyncJobCacheService.put(toResponse(asyncJobRepository.save(job)));
    }

    private void markSucceeded(UUID jobId, String resultJson) {
        AsyncJob job = getJob(jobId);
        job.markSucceeded(resultJson);
        asyncJobCacheService.put(toResponse(asyncJobRepository.save(job)));
    }

    private void markFailed(UUID jobId, Exception exception) {
        AsyncJob job = getJob(jobId);
        job.markFailed(exception.getMessage() == null ? "Async job failed." : exception.getMessage());
        asyncJobCacheService.put(toResponse(asyncJobRepository.save(job)));
    }

    private AsyncJob getJob(UUID jobId) {
        return asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Async job", jobId.toString()));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize async job result.", exception);
        }
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

    private com.fasterxml.jackson.databind.JsonNode readResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(resultJson);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode().put("raw", resultJson);
        }
    }
}
