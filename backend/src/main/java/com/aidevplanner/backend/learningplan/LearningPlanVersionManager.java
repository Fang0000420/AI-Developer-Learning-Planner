package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LearningPlanVersionManager {

    private final DailyTaskRepository dailyTaskRepository;
    private final ObjectMapper objectMapper;

    public LearningPlanVersionManager(
            DailyTaskRepository dailyTaskRepository,
            ObjectMapper objectMapper
    ) {
        this.dailyTaskRepository = dailyTaskRepository;
        this.objectMapper = objectMapper;
    }

    public void captureSnapshot(
            LearningPlan plan,
            List<DailyTask> tasks,
            String trigger,
            String reason,
            List<Integer> affectedDayIndexes
    ) {
        Map<String, Object> planJson = mutablePlanJson(plan);
        List<Map<String, Object>> history = mutableVersionHistory(planJson);
        int nextVersion = history.stream()
                .mapToInt(entry -> toInt(entry.get("version"), 0))
                .max()
                .orElse(0) + 1;
        Map<String, Object> previous = history.isEmpty() ? null : history.get(history.size() - 1);

        int dayCount = (int) tasks.stream().map(DailyTask::getDayIndex).distinct().count();
        int taskCount = tasks.size();
        int totalEstimatedMinutes = tasks.stream().mapToInt(DailyTask::getEstimatedMinutes).sum();
        int previousMinutes = previous == null ? totalEstimatedMinutes : toInt(previous.get("totalEstimatedMinutes"), totalEstimatedMinutes);
        int previousTaskCount = previous == null ? taskCount : toInt(previous.get("taskCount"), taskCount);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("version", nextVersion);
        snapshot.put("trigger", firstPresent(trigger, "update"));
        snapshot.put("reason", firstPresent(reason, "Plan updated."));
        snapshot.put("dayCount", dayCount);
        snapshot.put("taskCount", taskCount);
        snapshot.put("totalEstimatedMinutes", totalEstimatedMinutes);
        snapshot.put("minuteDelta", totalEstimatedMinutes - previousMinutes);
        snapshot.put("taskDelta", taskCount - previousTaskCount);
        snapshot.put("affectedDayIndexes", affectedDayIndexes == null ? List.of() : List.copyOf(affectedDayIndexes));
        snapshot.put("createdAt", LocalDateTime.now().toString());
        snapshot.put("days", serializeDays(tasks));
        history.add(snapshot);

        planJson.put("versionHistory", history);
        planJson.put("currentVersion", nextVersion);
        plan.setPlanJson(planJson);
    }

    public void restoreVersion(LearningPlan plan, Integer version) {
        Map<String, Object> planJson = mutablePlanJson(plan);
        List<Map<String, Object>> history = mutableVersionHistory(planJson);
        Map<String, Object> targetVersion = history.stream()
                .filter(entry -> toInt(entry.get("version"), -1) == version)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan version", version.longValue()));

        List<DailyTaskSnapshot> snapshots = objectMapper.convertValue(
                targetVersion.get("days"),
                new TypeReference<List<DailyTaskSnapshot>>() {
                }
        );
        dailyTaskRepository.deleteByPlanId(plan.getId());
        List<DailyTask> restoredTasks = new ArrayList<>();
        for (DailyTaskSnapshot day : snapshots) {
            for (DailyTaskSnapshotTask task : day.tasks()) {
                restoredTasks.add(new DailyTask(
                        plan,
                        plan.getUser(),
                        plan.getGoal(),
                        day.dayIndex(),
                        task.taskOrder(),
                        day.theme(),
                        task.title(),
                        task.description(),
                        task.estimatedMinutes(),
                        task.type(),
                        task.deliverable(),
                        task.priority()
                ));
            }
        }
        List<DailyTask> savedTasks = dailyTaskRepository.saveAll(restoredTasks);
        captureSnapshot(
                plan,
                savedTasks,
                "restore",
                "Restored from version " + version + ".",
                snapshots.stream().map(DailyTaskSnapshot::dayIndex).toList()
        );
    }

    public List<LearningPlanVersionSummaryResponse> versions(LearningPlan plan) {
        Map<String, Object> planJson = mutablePlanJson(plan);
        List<Map<String, Object>> history = mutableVersionHistory(planJson);
        int currentVersion = toInt(planJson.get("currentVersion"), 0);
        List<Map<String, Object>> ascending = history.stream()
                .sorted(Comparator.comparingInt(entry -> toInt(entry.get("version"), 0)))
                .toList();

        List<LearningPlanVersionSummaryResponse> responses = new ArrayList<>();
        for (int index = 0; index < ascending.size(); index++) {
            Map<String, Object> entry = ascending.get(index);
            Map<String, Object> previous = index == 0 ? null : ascending.get(index - 1);
            responses.add(new LearningPlanVersionSummaryResponse(
                    toInt(entry.get("version"), 0),
                    firstPresent((String) entry.get("trigger"), "update"),
                    firstPresent((String) entry.get("reason"), ""),
                    toInt(entry.get("dayCount"), 0),
                    toInt(entry.get("taskCount"), 0),
                    toInt(entry.get("totalEstimatedMinutes"), 0),
                    toInt(entry.get("minuteDelta"), 0),
                    toInt(entry.get("taskDelta"), 0),
                    toIntegerList(entry.get("affectedDayIndexes")),
                    diff(previous, entry),
                    toInt(entry.get("version"), 0) == currentVersion,
                    toDateTime(entry.get("createdAt"))
            ));
        }
        return responses.stream()
                .sorted(Comparator.comparingInt(response -> -response.version()))
                .toList();
    }

    private LearningPlanVersionDiffResponse diff(
            Map<String, Object> previous,
            Map<String, Object> current
    ) {
        Map<String, TaskVersionEntry> previousTasks = flattenTasks(previous == null ? null : previous.get("days"));
        Map<String, TaskVersionEntry> currentTasks = flattenTasks(current.get("days"));

        List<LearningPlanVersionTaskChangeResponse> changes = new ArrayList<>();
        LinkedHashSet<Integer> changedDays = new LinkedHashSet<>();
        int addedCount = 0;
        int removedCount = 0;
        int updatedCount = 0;

        for (Map.Entry<String, TaskVersionEntry> entry : currentTasks.entrySet()) {
            TaskVersionEntry currentTask = entry.getValue();
            TaskVersionEntry previousTask = previousTasks.remove(entry.getKey());
            if (previousTask == null) {
                addedCount++;
                changedDays.add(currentTask.dayIndex());
                changes.add(new LearningPlanVersionTaskChangeResponse(
                        currentTask.dayIndex(),
                        currentTask.title(),
                        "added",
                        null,
                        currentTask.estimatedMinutes()
                ));
                continue;
            }
            if (!sameTask(previousTask, currentTask)) {
                updatedCount++;
                changedDays.add(currentTask.dayIndex());
                changes.add(new LearningPlanVersionTaskChangeResponse(
                        currentTask.dayIndex(),
                        currentTask.title(),
                        "updated",
                        previousTask.estimatedMinutes(),
                        currentTask.estimatedMinutes()
                ));
            }
        }

        for (TaskVersionEntry removed : previousTasks.values()) {
            removedCount++;
            changedDays.add(removed.dayIndex());
            changes.add(new LearningPlanVersionTaskChangeResponse(
                    removed.dayIndex(),
                    removed.title(),
                    "removed",
                    removed.estimatedMinutes(),
                    null
            ));
        }

        return new LearningPlanVersionDiffResponse(
                addedCount,
                removedCount,
                updatedCount,
                List.copyOf(changedDays),
                changes.stream().limit(8).toList()
        );
    }

    private boolean sameTask(TaskVersionEntry previous, TaskVersionEntry current) {
        return previous.dayIndex().equals(current.dayIndex())
                && previous.title().equals(current.title())
                && previous.description().equals(current.description())
                && previous.estimatedMinutes().equals(current.estimatedMinutes())
                && previous.type().equals(current.type())
                && previous.deliverable().equals(current.deliverable())
                && previous.priority().equals(current.priority());
    }

    private Map<String, TaskVersionEntry> flattenTasks(Object daysValue) {
        if (daysValue == null) {
            return Map.of();
        }
        List<DailyTaskSnapshot> snapshots = objectMapper.convertValue(
                daysValue,
                new TypeReference<List<DailyTaskSnapshot>>() {
                }
        );
        Map<String, TaskVersionEntry> values = new LinkedHashMap<>();
        for (DailyTaskSnapshot day : snapshots) {
            if (day.tasks() == null) {
                continue;
            }
            for (DailyTaskSnapshotTask task : day.tasks()) {
                String key = day.dayIndex() + "#" + task.taskOrder() + "#" + firstPresent(task.title());
                values.put(key, new TaskVersionEntry(
                        day.dayIndex(),
                        firstPresent(task.title()),
                        firstPresent(task.description()),
                        task.estimatedMinutes() == null ? 0 : task.estimatedMinutes(),
                        firstPresent(task.type()),
                        firstPresent(task.deliverable()),
                        firstPresent(task.priority())
                ));
            }
        }
        return values;
    }

    private List<Map<String, Object>> serializeDays(List<DailyTask> tasks) {
        Map<Integer, List<DailyTask>> byDay = tasks.stream()
                .sorted(Comparator.comparing(DailyTask::getDayIndex).thenComparing(DailyTask::getTaskOrder))
                .collect(Collectors.groupingBy(
                        DailyTask::getDayIndex,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Map.Entry<Integer, List<DailyTask>> entry : byDay.entrySet()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("dayIndex", entry.getKey());
            day.put("theme", entry.getValue().isEmpty() ? "" : entry.getValue().get(0).getDayTheme());
            day.put("tasks", entry.getValue().stream().map(task -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("taskOrder", task.getTaskOrder());
                item.put("title", task.getTitle());
                item.put("description", task.getDescription());
                item.put("estimatedMinutes", task.getEstimatedMinutes());
                item.put("type", task.getType());
                item.put("deliverable", task.getDeliverable());
                item.put("priority", task.getPriority());
                return item;
            }).toList());
            serialized.add(day);
        }
        return serialized;
    }

    private Map<String, Object> mutablePlanJson(LearningPlan plan) {
        return plan.getPlanJson() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(plan.getPlanJson());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mutableVersionHistory(Map<String, Object> planJson) {
        Object existing = planJson.get("versionHistory");
        if (existing instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map<?, ?>)
                    .map(item -> new LinkedHashMap<>((Map<String, Object>) item))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return LocalDateTime.parse(text);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> toIntegerList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Integer> normalized = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                normalized.add(number.intValue());
            } else if (item instanceof String text && !text.isBlank()) {
                try {
                    normalized.add(Integer.parseInt(text.trim()));
                } catch (NumberFormatException ignored) {
                    // ignore invalid values
                }
            }
        }
        return normalized;
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private record DailyTaskSnapshot(
            Integer dayIndex,
            String theme,
            List<DailyTaskSnapshotTask> tasks
    ) {
    }

    private record DailyTaskSnapshotTask(
            Integer taskOrder,
            String title,
            String description,
            Integer estimatedMinutes,
            String type,
            String deliverable,
            String priority
    ) {
    }

    private record TaskVersionEntry(
            Integer dayIndex,
            String title,
            String description,
            Integer estimatedMinutes,
            String type,
            String deliverable,
            String priority
    ) {
    }
}
