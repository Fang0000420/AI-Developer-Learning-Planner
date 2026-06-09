package com.aidevplanner.backend.learningplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {

    List<LearningPlan> findAllByOrderByCreatedAtDesc();

    Optional<LearningPlan> findFirstByGoalIdOrderByCreatedAtDesc(Long goalId);
}
