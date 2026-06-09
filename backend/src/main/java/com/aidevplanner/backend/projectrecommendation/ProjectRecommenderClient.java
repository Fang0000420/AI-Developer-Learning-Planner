package com.aidevplanner.backend.projectrecommendation;

public interface ProjectRecommenderClient {

    ProjectRecommendResponse recommend(ProjectRecommendRequest request);
}
