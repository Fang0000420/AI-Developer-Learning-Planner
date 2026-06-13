package com.aidevplanner.backend.learningplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findByPlanIdOrderByDayIndexAscTaskOrderAsc(Long planId);

    List<DailyTask> findByPlanIdAndDayIndexOrderByTaskOrderAsc(Long planId, Integer dayIndex);

    void deleteByPlanId(Long planId);
}
