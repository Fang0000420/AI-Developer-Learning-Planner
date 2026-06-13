package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.learningplan.LearningPlanGenerateRequest;
import com.aidevplanner.backend.path.PathAnalysisRequest;
import com.aidevplanner.backend.progress.ProgressSubmitRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Validated
public class AsyncJobController {

    private final AsyncJobService asyncJobService;

    public AsyncJobController(AsyncJobService asyncJobService) {
        this.asyncJobService = asyncJobService;
    }

    @PostMapping("/plan-generation")
    public ResponseEntity<AsyncJobResponse> createPlanGenerationJob(
            @Valid @RequestBody LearningPlanGenerateRequest request
    ) {
        AsyncJobResponse response = asyncJobService.createPlanGenerationJob(request);
        return ResponseEntity
                .created(URI.create("/api/jobs/" + response.jobId()))
                .body(response);
    }

    @PostMapping("/path-analysis")
    public ResponseEntity<AsyncJobResponse> createPathAnalysisJob(
            @Valid @RequestBody PathAnalysisRequest request
    ) {
        AsyncJobResponse response = asyncJobService.createPathAnalysisJob(request);
        return ResponseEntity
                .created(URI.create("/api/jobs/" + response.jobId()))
                .body(response);
    }

    @PostMapping("/progress-submission")
    public ResponseEntity<AsyncJobResponse> createProgressSubmissionJob(
            @Valid @RequestBody ProgressSubmitRequest request
    ) {
        AsyncJobResponse response = asyncJobService.createProgressSubmissionJob(request);
        return ResponseEntity
                .created(URI.create("/api/jobs/" + response.jobId()))
                .body(response);
    }

    @GetMapping("/{jobId}")
    public AsyncJobResponse getJob(@PathVariable UUID jobId) {
        return asyncJobService.getJob(jobId);
    }
}
