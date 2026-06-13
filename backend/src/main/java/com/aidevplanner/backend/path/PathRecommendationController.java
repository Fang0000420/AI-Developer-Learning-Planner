package com.aidevplanner.backend.path;

import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals/{goalId}/path-recommendation")
@Validated
public class PathRecommendationController {

    private final PathRecommendationService pathRecommendationService;

    public PathRecommendationController(PathRecommendationService pathRecommendationService) {
        this.pathRecommendationService = pathRecommendationService;
    }

    @GetMapping
    public ResponseEntity<PathRecommendationResponse> getLatestPathRecommendation(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        PathRecommendationResponse response = pathRecommendationService.getLatestPathRecommendation(goalId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze")
    public PathRecommendationResponse analyzeGoal(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        return pathRecommendationService.analyzeGoal(goalId);
    }
}
