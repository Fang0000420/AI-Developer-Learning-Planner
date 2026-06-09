package com.aidevplanner.backend.skillgap;

import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals/{goalId}/skill-gap")
@Validated
public class SkillGapAnalysisController {

    private final SkillGapAnalysisService skillGapAnalysisService;

    public SkillGapAnalysisController(SkillGapAnalysisService skillGapAnalysisService) {
        this.skillGapAnalysisService = skillGapAnalysisService;
    }

    @GetMapping
    public ResponseEntity<SkillGapAnalysisResponse> getLatestSkillGapAnalysis(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        SkillGapAnalysisResponse response = skillGapAnalysisService.getLatestSkillGapAnalysis(goalId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze")
    public SkillGapAnalysisResponse analyzeSkillGap(
            @Positive(message = "Goal id must be positive.")
            @PathVariable
            Long goalId
    ) {
        return skillGapAnalysisService.analyzeSkillGap(goalId);
    }
}
