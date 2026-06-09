package com.aidevplanner.backend.learningplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {

    Optional<LearningPlan> findFirstByGoalIdOrderByCreatedAtDesc(Long goalId);
}
