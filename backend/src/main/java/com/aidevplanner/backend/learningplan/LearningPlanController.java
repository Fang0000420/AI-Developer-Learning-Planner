package com.aidevplanner.backend.learningplan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@Validated
public class LearningPlanController {

    private final LearningPlanService learningPlanService;

    public LearningPlanController(LearningPlanService learningPlanService) {
        this.learningPlanService = learningPlanService;
    }

    @GetMapping
    public List<LearningPlanSummaryResponse> listPlans() {
        return learningPlanService.listPlans();
    }

    @PostMapping("/generate")
    public LearningPlanResponse generatePlan(@Valid @RequestBody LearningPlanGenerateRequest request) {
        return learningPlanService.generatePlan(request.goalId());
    }

    @GetMapping("/{planId}")
    public LearningPlanResponse getPlan(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId
    ) {
        return learningPlanService.getPlan(planId);
    }
}
