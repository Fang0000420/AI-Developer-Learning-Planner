package com.aidevplanner.backend.goaldecomposition;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface GoalDecomposerClient {

    AgentClientResponse<GoalDecomposeResponse> decompose(GoalDecomposeRequest request);
}
