package com.aidevplanner.backend.progress;

public interface ProgressReviewerClient {

    ProgressReviewAgentResponse review(ProgressReviewAgentRequest request);
}
