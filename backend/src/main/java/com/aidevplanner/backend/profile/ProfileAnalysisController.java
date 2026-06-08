package com.aidevplanner.backend.profile;

import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals/{goalId}/profile")
@Validated
public class ProfileAnalysisController {

    private final ProfileAnalysisService profileAnalysisService;

    public ProfileAnalysisController(ProfileAnalysisService profileAnalysisService) {
        this.profileAnalysisService = profileAnalysisService;
    }

    @GetMapping
    public ResponseEntity<SkillProfileResponse> getLatestProfile(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        SkillProfileResponse response = profileAnalysisService.getLatestProfile(goalId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze")
    public SkillProfileResponse analyzeGoal(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        return profileAnalysisService.analyzeGoal(goalId);
    }
}
