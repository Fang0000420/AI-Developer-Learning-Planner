package com.aidevplanner.backend.learningplan;

public interface PlanGeneratorClient {

    PlanGenerateAgentResponse generate(PlanGenerateAgentRequest request);
}
