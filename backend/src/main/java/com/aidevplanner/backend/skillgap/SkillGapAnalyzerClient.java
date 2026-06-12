package com.aidevplanner.backend.skillgap;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface SkillGapAnalyzerClient {

    AgentClientResponse<SkillGapAnalyzeResponse> analyze(SkillGapAnalyzeRequest request);
}
