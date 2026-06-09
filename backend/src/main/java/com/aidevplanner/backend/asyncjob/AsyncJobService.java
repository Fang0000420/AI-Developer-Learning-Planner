package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.learningplan.LearningPlanGenerateRequest;
import com.aidevplanner.backend.progress.ProgressSubmitRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AsyncJobService {

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobRunner asyncJobRunner;
    private final ObjectMapper objectMapper;

    public AsyncJobService(
            AsyncJobRepository asyncJobRepository,
            AsyncJobRunner asyncJobRunner,
            ObjectMapper objectMapper
    ) {
        this.asyncJobRepository = asyncJobRepository;
        this.asyncJobRunner = asyncJobRunner;
        this.objectMapper = objectMapper;
    }

    public AsyncJobResponse createPlanGenerationJob(LearningPlanGenerateRequest request) {
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PLAN_GENERATION,
                writeJson(request)
        ));
        asyncJobRunner.runPlanGeneration(job.getId(), request.goalId());
        return toResponse(job);
    }

    public AsyncJobResponse createProgressSubmissionJob(ProgressSubmitRequest request) {
        AsyncJob job = asyncJobRepository.save(new AsyncJob(
                UUID.randomUUID(),
                AsyncJobType.PROGRESS_SUBMISSION,
                writeJson(request)
        ));
        asyncJobRunner.runProgressSubmission(job.getId(), request);
        return toResponse(job);
    }

    public AsyncJobResponse getJob(UUID jobId) {
        AsyncJob job = asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Async job", jobId.toString()));
        return toResponse(job);
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
