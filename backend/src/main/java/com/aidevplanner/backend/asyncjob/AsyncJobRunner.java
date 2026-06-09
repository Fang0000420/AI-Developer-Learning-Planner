package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.learningplan.LearningPlanResponse;
import com.aidevplanner.backend.learningplan.LearningPlanService;
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

    private final AsyncJobRepository asyncJobRepository;
    private final LearningPlanService learningPlanService;
    private final ObjectMapper objectMapper;
    private final ProgressLogService progressLogService;

    public AsyncJobRunner(
            AsyncJobRepository asyncJobRepository,
            LearningPlanService learningPlanService,
            ObjectMapper objectMapper,
            ProgressLogService progressLogService
    ) {
        this.asyncJobRepository = asyncJobRepository;
        this.learningPlanService = learningPlanService;
        this.objectMapper = objectMapper;
        this.progressLogService = progressLogService;
    }

    @Async
    public void runPlanGeneration(UUID jobId, Long goalId) {
        markRunning(jobId);
        try {
            LearningPlanResponse response = learningPlanService.generatePlan(goalId);
            markSucceeded(jobId, writeJson(response));
        } catch (Exception exception) {
            markFailed(jobId, exception);
        }
    }

    @Async
    public void runProgressSubmission(UUID jobId, ProgressSubmitRequest request) {
        markRunning(jobId);
        try {
            ProgressLogResponse response = progressLogService.submitProgress(request);
            markSucceeded(jobId, writeJson(response));
        } catch (Exception exception) {
            markFailed(jobId, exception);
        }
    }

    private void markRunning(UUID jobId) {
        AsyncJob job = getJob(jobId);
        job.markRunning();
        asyncJobRepository.save(job);
    }

    private void markSucceeded(UUID jobId, String resultJson) {
        AsyncJob job = getJob(jobId);
        job.markSucceeded(resultJson);
        asyncJobRepository.save(job);
    }

    private void markFailed(UUID jobId, Exception exception) {
        AsyncJob job = getJob(jobId);
        job.markFailed(exception.getMessage() == null ? "Async job failed." : exception.getMessage());
        asyncJobRepository.save(job);
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
}
