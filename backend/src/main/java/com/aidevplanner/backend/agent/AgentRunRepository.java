package com.aidevplanner.backend.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long>, JpaSpecificationExecutor<AgentRun> {

    Optional<AgentRun> findFirstByGoalIdAndAgentNameAndStatusOrderByCreatedAtDesc(
            Long goalId,
            String agentName,
            AgentRunStatus status
    );

    Optional<AgentRun> findByIdAndUserId(Long id, Long userId);
}
