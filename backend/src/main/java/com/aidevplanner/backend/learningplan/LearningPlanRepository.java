package com.aidevplanner.backend.learningplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {

    List<LearningPlan> findAllByOrderByCreatedAtDesc();

    List<LearningPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LearningPlan> findFirstByGoalIdOrderByCreatedAtDesc(Long goalId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
