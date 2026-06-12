package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface PlanGeneratorClient {

    AgentClientResponse<PlanGenerateAgentResponse> generate(PlanGenerateAgentRequest request);
}
