package com.aidevplanner.backend.goaldecomposition;

public interface GoalDecomposerClient {

    GoalDecomposeResponse decompose(GoalDecomposeRequest request);
}
