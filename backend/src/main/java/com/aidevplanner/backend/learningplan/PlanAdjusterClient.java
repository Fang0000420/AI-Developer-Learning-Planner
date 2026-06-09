package com.aidevplanner.backend.learningplan;

public interface PlanAdjusterClient {

    PlanAdjustAgentResponse adjust(PlanAdjustAgentRequest request);
}
