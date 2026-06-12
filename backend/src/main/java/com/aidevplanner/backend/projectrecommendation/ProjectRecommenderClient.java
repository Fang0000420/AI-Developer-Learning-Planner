package com.aidevplanner.backend.projectrecommendation;

import com.aidevplanner.backend.agent.AgentClientResponse;

public interface ProjectRecommenderClient {

    AgentClientResponse<ProjectRecommendResponse> recommend(ProjectRecommendRequest request);
}
