package com.aidevplanner.backend.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    Optional<AgentRun> findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
            Long goalId,
            String agentName,
            AgentRunStatus status
    );
}
