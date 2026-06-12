package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface PlanAdjusterClient {

    AgentClientResponse<PlanAdjustAgentResponse> adjust(PlanAdjustAgentRequest request);
}
