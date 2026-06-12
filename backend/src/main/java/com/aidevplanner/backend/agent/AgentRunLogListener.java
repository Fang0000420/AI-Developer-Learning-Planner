package com.aidevplanner.backend.agent;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentRunLogListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRunLogListener.class);

    @PostPersist
    @PostUpdate
    void logAgentRun(AgentRun run) {
        Long goalId = run.getGoal() == null ? null : run.getGoal().getId();
        Long planId = run.getPlan() == null ? null : run.getPlan().getId();
        if (run.getStatus() == AgentRunStatus.FAILED) {
            LOGGER.warn(
                    "agent run recorded runId={} agentName={} status={} responseSource={} goalId={} planId={} latencyMs={} requestId={} error={}",
                    run.getId(),
                    run.getAgentName(),
                    run.getStatus(),
                    run.getResponseSource(),
                    goalId,
                    planId,
                    run.getLatencyMs(),
                    run.getRequestId(),
                    run.getErrorMessage()
            );
            return;
        }
        LOGGER.info(
                "agent run recorded runId={} agentName={} status={} responseSource={} goalId={} planId={} latencyMs={} requestId={}",
                run.getId(),
                run.getAgentName(),
                run.getStatus(),
                run.getResponseSource(),
                goalId,
                planId,
                run.getLatencyMs(),
                run.getRequestId()
        );
    }
}
