package com.aidevplanner.backend.agent;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentRunQueryService {

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ObjectMapper objectMapper;

    public AgentRunQueryService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            ObjectMapper objectMapper
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AgentRunSummaryResponse> listRuns(Long goalId, Long planId, String agentName) {
        Specification<AgentRun> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            authenticatedUserService.currentUserId()
                    .ifPresent(userId -> predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId)));
            if (goalId != null) {
                predicates.add(criteriaBuilder.equal(root.get("goal").get("id"), goalId));
            }
            if (planId != null) {
                predicates.add(criteriaBuilder.equal(root.get("plan").get("id"), planId));
            }
            if (agentName != null && !agentName.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("agentName"), agentName.trim()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return agentRunRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentRunDetailResponse getRun(Long runId) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        AgentRun run = currentUserId == null
                ? agentRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run", runId))
                : agentRunRepository.findByIdAndUserId(runId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run", runId));
        return toDetailResponse(run);
    }

    private AgentRunSummaryResponse toSummaryResponse(AgentRun run) {
        return new AgentRunSummaryResponse(
                run.getId(),
                run.getUser() == null ? null : run.getUser().getId(),
                run.getGoal() == null ? null : run.getGoal().getId(),
                run.getPlan() == null ? null : run.getPlan().getId(),
                run.getAgentName(),
                run.getStatus(),
                run.getLatencyMs(),
                run.getErrorMessage(),
                run.getRequestId(),
                run.getCreatedAt()
        );
    }

    private AgentRunDetailResponse toDetailResponse(AgentRun run) {
        return new AgentRunDetailResponse(
                run.getId(),
                run.getUser() == null ? null : run.getUser().getId(),
                run.getGoal() == null ? null : run.getGoal().getId(),
                run.getPlan() == null ? null : run.getPlan().getId(),
                run.getAgentName(),
                run.getStatus(),
                run.getLatencyMs(),
                run.getErrorMessage(),
                run.getRequestId(),
                readJson(run.getInputJson()),
                readJson(run.getOutputJson()),
                run.getCreatedAt()
        );
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode().put("raw", value);
        }
    }
}
