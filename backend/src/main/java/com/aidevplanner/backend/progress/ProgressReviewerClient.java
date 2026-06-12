package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface ProgressReviewerClient {

    AgentClientResponse<ProgressReviewAgentResponse> review(ProgressReviewAgentRequest request);
}
