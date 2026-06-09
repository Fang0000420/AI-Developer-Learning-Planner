package com.aidevplanner.backend.projectrecommendation;

import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals/{goalId}/project-recommendation")
@Validated
public class ProjectRecommendationController {

    private final ProjectRecommendationService projectRecommendationService;

    public ProjectRecommendationController(ProjectRecommendationService projectRecommendationService) {
        this.projectRecommendationService = projectRecommendationService;
    }

    @GetMapping
    public ResponseEntity<ProjectRecommendationResponse> getLatestProjectRecommendation(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        ProjectRecommendationResponse response =
                projectRecommendationService.getLatestProjectRecommendation(goalId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recommend")
    public ProjectRecommendationResponse recommendProject(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        return projectRecommendationService.recommendProject(goalId);
    }
}
