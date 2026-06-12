package com.aidevplanner.backend.profile;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface ProfileAnalyzerClient {

    AgentClientResponse<ProfileAnalyzeResponse> analyze(ProfileAnalyzeRequest request);
}
