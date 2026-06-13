package com.aidevplanner.backend.progress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@Validated
public class ProgressLogController {

    private final ProgressLogService progressLogService;

    public ProgressLogController(ProgressLogService progressLogService) {
        this.progressLogService = progressLogService;
    }

    @PostMapping
    public ProgressLogResponse submitProgress(@Valid @RequestBody ProgressSubmitRequest request) {
        return progressLogService.submitProgress(request);
    }

    @GetMapping("/{planId}")
    public List<ProgressLogResponse> listProgress(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId,
            @Positive(message = "Day index must be positive.")
            @RequestParam(required = false)
            Integer dayIndex
    ) {
        return progressLogService.listProgress(planId, dayIndex);
    }

    @GetMapping("/{planId}/adaptive-schedule")
    public AdaptiveScheduleControlResponse getAdaptiveScheduleControl(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId
    ) {
        return progressLogService.getAdaptiveScheduleControl(planId);
    }

    @PatchMapping("/{planId}/adaptive-schedule")
    public AdaptiveScheduleControlResponse overrideAdaptiveSchedule(
            @Positive(message = "Plan id must be positive.")
            @PathVariable
            Long planId,
            @Valid @RequestBody AdaptiveScheduleOverrideRequest request
    ) {
        return progressLogService.overrideAdaptiveSchedule(planId, request);
    }
}
