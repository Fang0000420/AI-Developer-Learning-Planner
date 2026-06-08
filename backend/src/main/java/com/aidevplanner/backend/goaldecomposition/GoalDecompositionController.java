package com.aidevplanner.backend.goaldecomposition;

import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals/{goalId}/decomposition")
@Validated
public class GoalDecompositionController {

    private final GoalDecompositionService goalDecompositionService;

    public GoalDecompositionController(GoalDecompositionService goalDecompositionService) {
        this.goalDecompositionService = goalDecompositionService;
    }

    @GetMapping
    public ResponseEntity<GoalDecompositionResponse> getLatestDecomposition(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        GoalDecompositionResponse response = goalDecompositionService.getLatestDecomposition(goalId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/decompose")
    public GoalDecompositionResponse decomposeGoal(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        return goalDecompositionService.decomposeGoal(goalId);
    }
}
