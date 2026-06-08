package com.aidevplanner.backend.goal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalCreateRequest request) {
        GoalResponse response = goalService.createGoal(request);
        return ResponseEntity
                .created(URI.create("/api/goals/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<GoalResponse> listGoals(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) GoalStatus status
    ) {
        return goalService.listGoals(userId, status);
    }

    @GetMapping("/{goalId}")
    public GoalResponse getGoal(@PathVariable Long goalId) {
        return goalService.getGoal(goalId);
    }

    @PutMapping("/{goalId}")
    public GoalResponse updateGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalUpdateRequest request
    ) {
        return goalService.updateGoal(goalId, request);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }
}
