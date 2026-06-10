package com.aidevplanner.backend.agent;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-runs")
@Validated
public class AgentRunController {

    private final AgentRunQueryService agentRunQueryService;

    public AgentRunController(AgentRunQueryService agentRunQueryService) {
        this.agentRunQueryService = agentRunQueryService;
    }

    @GetMapping
    public List<AgentRunSummaryResponse> listRuns(
            @RequestParam(required = false) @Positive(message = "Goal id must be positive.") Long goalId,
            @RequestParam(required = false) @Positive(message = "Plan id must be positive.") Long planId,
            @RequestParam(required = false) String agentName
    ) {
        return agentRunQueryService.listRuns(goalId, planId, agentName);
    }

    @GetMapping("/{runId}")
    public AgentRunDetailResponse getRun(
            @PathVariable @Positive(message = "Agent run id must be positive.") Long runId
    ) {
        return agentRunQueryService.getRun(runId);
    }
}
