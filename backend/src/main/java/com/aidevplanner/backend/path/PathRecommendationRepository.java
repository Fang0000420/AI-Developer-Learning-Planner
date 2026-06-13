package com.aidevplanner.backend.path;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PathRecommendationRepository extends JpaRepository<PathRecommendation, Long> {

    Optional<PathRecommendation> findFirstByGoalIdOrderByCreatedAtDesc(Long goalId);

    Optional<PathRecommendation> findFirstByGoalIdOrderByVersionDesc(Long goalId);
}
