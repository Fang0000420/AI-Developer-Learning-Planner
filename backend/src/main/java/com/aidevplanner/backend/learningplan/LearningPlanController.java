package com.aidevplanner.backend.learningplan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/{planId}/tasks")
    public List<PlanTaskResponse> listTasks(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId
    ) {
        return learningPlanService.listTasks(planId);
    }

    @GetMapping("/{planId}/tasks/today")
    public PlanDayResponse getTodayTasks(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId,
            @Positive(message = "Day index must be positive.")
            @RequestParam(defaultValue = "1")
            Integer dayIndex
    ) {
        return learningPlanService.getDayTasks(planId, dayIndex);
    }

    @PutMapping("/{planId}")
    public LearningPlanResponse updatePlanStatus(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId,
            @Valid @RequestBody LearningPlanUpdateRequest request
    ) {
        return learningPlanService.updatePlanStatus(planId, request);
    }

    @PutMapping("/{planId}/tasks/{taskId}/status")
    public PlanTaskResponse updateTaskStatus(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId,
            @Positive(message = "Task id must be positive.")
            @PathVariable
            Long taskId,
            @Valid @RequestBody DailyTaskStatusUpdateRequest request
    ) {
        return learningPlanService.updateTaskStatus(planId, taskId, request);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId
    ) {
        learningPlanService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }
}
