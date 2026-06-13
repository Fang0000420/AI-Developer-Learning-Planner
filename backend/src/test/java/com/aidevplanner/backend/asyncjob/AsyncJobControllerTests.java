package com.aidevplanner.backend.asyncjob;

import com.aidevplanner.backend.learningplan.LearningPlanGenerateRequest;
import com.aidevplanner.backend.path.PathAnalysisRequest;
import com.aidevplanner.backend.progress.ProgressSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsyncJobController.class)
class AsyncJobControllerTests {

    private static final UUID JOB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 6, 9, 16, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AsyncJobService asyncJobService;

    @Test
    void createsPlanGenerationJob() throws Exception {
        LearningPlanGenerateRequest request = new LearningPlanGenerateRequest(10L);
        when(asyncJobService.createPlanGenerationJob(request)).thenReturn(pendingPlanJob());

        mockMvc.perform(post("/api/jobs/plan-generation")
                        .contentType("application/json")
                        .content("{\"goalId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/jobs/" + JOB_ID))
                .andExpect(jsonPath("$.jobId").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.jobType").value("PLAN_GENERATION"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(asyncJobService).createPlanGenerationJob(request);
    }

    @Test
    void createsPathAnalysisJob() throws Exception {
        PathAnalysisRequest request = new PathAnalysisRequest(10L);
        when(asyncJobService.createPathAnalysisJob(request)).thenReturn(pendingPathJob());

        mockMvc.perform(post("/api/jobs/path-analysis")
                        .contentType("application/json")
                        .content("{\"goalId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/jobs/" + JOB_ID))
                .andExpect(jsonPath("$.jobId").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.jobType").value("PATH_ANALYSIS"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(asyncJobService).createPathAnalysisJob(request);
    }

    @Test
    void createsProgressSubmissionJob() throws Exception {
        ProgressSubmitRequest request = new ProgressSubmitRequest(
                30L,
                1,
                "Finished the first task.",
                List.of(1L),
                List.of(2L),
                List.of("Need more UI time")
        );
        when(asyncJobService.createProgressSubmissionJob(request)).thenReturn(pendingProgressJob());

        mockMvc.perform(post("/api/jobs/progress-submission")
                        .contentType("application/json")
                        .content("""
                                {
                                  "planId": 30,
                                  "dayIndex": 1,
                                  "userFeedback": "Finished the first task.",
                                  "completedTaskIds": [1],
                                  "unfinishedTaskIds": [2],
                                  "blockers": ["Need more UI time"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/jobs/" + JOB_ID))
                .andExpect(jsonPath("$.jobType").value("PROGRESS_SUBMISSION"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(asyncJobService).createProgressSubmissionJob(request);
    }

    @Test
    void returnsJobStatus() throws Exception {
        when(asyncJobService.getJob(JOB_ID)).thenReturn(succeededPlanJob());

        mockMvc.perform(get("/api/jobs/{jobId}", JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.id").value(50));

        verify(asyncJobService).getJob(JOB_ID);
    }

    @Test
    void rejectsMissingGoalId() throws Exception {
        mockMvc.perform(post("/api/jobs/plan-generation")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.goalId").value("Goal id is required."));

        verifyNoInteractions(asyncJobService);
    }

    private AsyncJobResponse pendingPlanJob() {
        return new AsyncJobResponse(
                JOB_ID,
                AsyncJobType.PLAN_GENERATION,
                AsyncJobStatus.PENDING,
                null,
                null,
                TIMESTAMP,
                TIMESTAMP
        );
    }

    private AsyncJobResponse pendingPathJob() {
        return new AsyncJobResponse(
                JOB_ID,
                AsyncJobType.PATH_ANALYSIS,
                AsyncJobStatus.PENDING,
                null,
                null,
                TIMESTAMP,
                TIMESTAMP
        );
    }

    private AsyncJobResponse pendingProgressJob() {
        return new AsyncJobResponse(
                JOB_ID,
                AsyncJobType.PROGRESS_SUBMISSION,
                AsyncJobStatus.PENDING,
                null,
                null,
                TIMESTAMP,
                TIMESTAMP
        );
    }

    private AsyncJobResponse succeededPlanJob() {
        return new AsyncJobResponse(
                JOB_ID,
                AsyncJobType.PLAN_GENERATION,
                AsyncJobStatus.SUCCEEDED,
                objectMapper.createObjectNode()
                        .put("id", 50)
                        .put("planTitle", "Async generated plan"),
                null,
                TIMESTAMP,
                TIMESTAMP
        );
    }
}
