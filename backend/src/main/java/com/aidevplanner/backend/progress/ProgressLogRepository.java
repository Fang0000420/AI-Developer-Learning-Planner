package com.aidevplanner.backend.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {

    List<ProgressLog> findByPlanIdOrderByCreatedAtDesc(Long planId);

    List<ProgressLog> findByPlanIdAndDayIndexOrderByCreatedAtDesc(Long planId, Integer dayIndex);

    List<ProgressLog> findByPlanIdAndUserIdOrderByCreatedAtDesc(Long planId, Long userId);

    List<ProgressLog> findByPlanIdAndUserIdAndDayIndexOrderByCreatedAtDesc(Long planId, Long userId, Integer dayIndex);
}
