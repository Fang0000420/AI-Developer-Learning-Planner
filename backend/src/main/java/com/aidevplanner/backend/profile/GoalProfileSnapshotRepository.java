package com.aidevplanner.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalProfileSnapshotRepository extends JpaRepository<GoalProfileSnapshot, Long> {

    List<GoalProfileSnapshot> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GoalProfileSnapshot> findFirstByGoalIdOrderByCreatedAtDesc(Long goalId);
}
