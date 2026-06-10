package com.aidevplanner.backend.asyncjob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.aidevplanner.backend.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "async_jobs")
public class AsyncJob {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private AsyncJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AsyncJobStatus status;

    @Column(name = "input_json", nullable = false, columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    protected AsyncJob() {
    }

    public AsyncJob(UUID id, AsyncJobType jobType, String inputJson) {
        this(id, jobType, inputJson, null);
    }

    public AsyncJob(UUID id, AsyncJobType jobType, String inputJson, User user) {
        this.id = id;
        this.jobType = jobType;
        this.inputJson = inputJson;
        this.user = user;
        this.status = AsyncJobStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public AsyncJobType getJobType() {
        return jobType;
    }

    public AsyncJobStatus getStatus() {
        return status;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markRunning() {
        this.status = AsyncJobStatus.RUNNING;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markSucceeded(String resultJson) {
        this.status = AsyncJobStatus.SUCCEEDED;
        this.resultJson = resultJson;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = AsyncJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}
