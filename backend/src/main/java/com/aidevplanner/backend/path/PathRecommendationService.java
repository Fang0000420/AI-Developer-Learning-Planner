package com.aidevplanner.backend.path;

import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.agent.AgentRunRepository;
import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalRepository;
import com.aidevplanner.backend.goaldecomposition.GoalDecompositionResponse;
import com.aidevplanner.backend.goaldecomposition.GoalDecompositionService;
import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;
import com.aidevplanner.backend.knowledge.KnowledgeBasisResponse;
import com.aidevplanner.backend.knowledge.KnowledgeContextBundle;
import com.aidevplanner.backend.knowledge.KnowledgeContextService;
import com.aidevplanner.backend.profile.ProfileAnalysisService;
import com.aidevplanner.backend.profile.SkillProfileResponse;
import com.aidevplanner.backend.projectrecommendation.ProjectRecommendationResponse;
import com.aidevplanner.backend.projectrecommendation.ProjectRecommendationService;
import com.aidevplanner.backend.skillgap.SkillGapAnalysisResponse;
import com.aidevplanner.backend.skillgap.SkillGapAnalysisService;
import com.aidevplanner.backend.skillgap.SkillGapResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PathRecommendationService {

    private final AgentRunRepository agentRunRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalDecompositionService goalDecompositionService;
    private final GoalRepository goalRepository;
    private final KnowledgeContextService knowledgeContextService;
    private final PathRecommendationRepository pathRecommendationRepository;
    private final ProfileAnalysisService profileAnalysisService;
    private final ProjectRecommendationService projectRecommendationService;
    private final SkillGapAnalysisService skillGapAnalysisService;

    public PathRecommendationService(
            AgentRunRepository agentRunRepository,
            AuthenticatedUserService authenticatedUserService,
            GoalDecompositionService goalDecompositionService,
            GoalRepository goalRepository,
            KnowledgeContextService knowledgeContextService,
            PathRecommendationRepository pathRecommendationRepository,
            ProfileAnalysisService profileAnalysisService,
            ProjectRecommendationService projectRecommendationService,
            SkillGapAnalysisService skillGapAnalysisService
    ) {
        this.agentRunRepository = agentRunRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.goalDecompositionService = goalDecompositionService;
        this.goalRepository = goalRepository;
        this.knowledgeContextService = knowledgeContextService;
        this.pathRecommendationRepository = pathRecommendationRepository;
        this.profileAnalysisService = profileAnalysisService;
        this.projectRecommendationService = projectRecommendationService;
        this.skillGapAnalysisService = skillGapAnalysisService;
    }

    @Transactional(readOnly = true)
    public PathRecommendationResponse getLatestPathRecommendation(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);
        return pathRecommendationRepository.findFirstByGoalIdOrderByCreatedAtDesc(goalId)
                .map(recommendation -> toResponse(goal, recommendation))
                .orElse(null);
    }

    @Transactional
    public PathRecommendationResponse analyzeGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        ensureCurrentUserOwns(goal);

        SkillProfileResponse profile = profileAnalysisService.analyzeGoal(goalId);
        GoalDecompositionResponse decomposition = goalDecompositionService.decomposeGoal(goalId);
        SkillGapAnalysisResponse skillGapAnalysis = skillGapAnalysisService.analyzeSkillGap(goalId);
        ProjectRecommendationResponse projectRecommendation = projectRecommendationService.recommendProject(goalId);
        KnowledgeContextBundle knowledgeContext = knowledgeContextService.buildForGoal(goal);

        AgentRun sourceAgentRun = projectRecommendation.runId() == null
                ? null
                : agentRunRepository.findById(projectRecommendation.runId()).orElse(null);

        PathRecommendation savedRecommendation = pathRecommendationRepository.save(
                new PathRecommendation(
                        goal.getUser(),
                        goal,
                        sourceAgentRun,
                        nextVersion(goalId),
                        firstPresent(
                                projectRecommendation.recommendedProject(),
                                fallbackTrackTitle(goal)
                        ),
                        firstPresent(
                                projectRecommendation.reason(),
                                defaultSummary(goal)
                        ),
                        currentPosition(goal, profile),
                        nextStep(goal, decomposition, skillGapAnalysis),
                        firstPresent(projectRecommendation.difficulty(), defaultDifficulty(goal)),
                        positiveOrDefault(projectRecommendation.durationDays(), goal.getDurationDays()),
                        positiveOrDefault(
                                projectRecommendation.dailyTimeHours(),
                                goal.getUser().getDailyAvailableHours()
                        ),
                        cleanList(projectRecommendation.coreTechStack(), defaultFocusAreas(goal)),
                        milestones(goal, decomposition, projectRecommendation),
                        riskSignals(goal, profile, skillGapAnalysis),
                        evidence(goal, profile, skillGapAnalysis, knowledgeContext),
                        cleanList(projectRecommendation.finalDeliverables(), defaultDeliverables(goal))
                )
        );

        return toResponse(goal, savedRecommendation);
    }

    private PathRecommendationResponse toResponse(Goal goal, PathRecommendation recommendation) {
        KnowledgeBasisResponse knowledgeBasis = knowledgeContextService.basisForGoal(goal, recommendation.getEvidence());
        return PathRecommendationResponse.from(recommendation, knowledgeBasis);
    }

    private int nextVersion(Long goalId) {
        return pathRecommendationRepository.findFirstByGoalIdOrderByVersionDesc(goalId)
                .map(PathRecommendation::getVersion)
                .map(version -> version + 1)
                .orElse(1);
    }

    private void ensureCurrentUserOwns(Goal goal) {
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!goal.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Goal", goal.getId());
            }
        });
    }

    private String currentPosition(Goal goal, SkillProfileResponse profile) {
        boolean zh = isZh(goal);
        String skills = joinTop(cleanList(profile.currentSkills(), List.of()), 3);
        String strengths = joinTop(cleanList(profile.strengths(), List.of()), 2);
        String weaknesses = joinTop(cleanList(profile.weaknesses(), List.of()), 2);

        if (zh) {
            return "当前已有基础主要体现在 " + fallbackText(skills, "目标相关基础")
                    + "；已有优势包括 " + fallbackText(strengths, "学习目标明确与持续投入")
                    + "；接下来需要重点补齐 " + fallbackText(weaknesses, "系统化练习与成果验证") + "。";
        }

        return "The current foundation shows up most clearly in "
                + fallbackText(skills, "goal-related fundamentals")
                + "; existing strengths include "
                + fallbackText(strengths, "clear intent and steady commitment")
                + "; the next priority is to strengthen "
                + fallbackText(weaknesses, "systematic practice and evidence of progress")
                + ".";
    }

    private String nextStep(
            Goal goal,
            GoalDecompositionResponse decomposition,
            SkillGapAnalysisResponse skillGapAnalysis
    ) {
        boolean zh = isZh(goal);
        Optional<SubGoalResponse> highPrioritySubGoal = decomposition.subGoals() == null
                ? Optional.empty()
                : decomposition.subGoals().stream()
                .filter(subGoal -> "high".equalsIgnoreCase(subGoal.priority()))
                .findFirst();
        if (highPrioritySubGoal.isPresent()) {
            SubGoalResponse subGoal = highPrioritySubGoal.get();
            return zh
                    ? "先围绕“" + subGoal.title() + "”建立稳定练习，并产出一个可验证的小成果。"
                    : "Start with " + subGoal.title() + " and turn it into a steady practice loop with one small, verifiable output.";
        }

        SkillGapResponse firstGap = firstSkillGap(skillGapAnalysis);
        if (firstGap != null) {
            return zh
                    ? "先补齐“" + firstGap.skill() + "”这一项关键差距，再进入更完整的应用任务。"
                    : "Close the gap in " + firstGap.skill() + " first before moving into broader applied work.";
        }

        return zh
                ? "先完成一轮聚焦练习，并记录结果，再决定下一步升级方向。"
                : "Complete one focused practice cycle, record the result, and use it to decide the next upgrade.";
    }

    private List<String> milestones(
            Goal goal,
            GoalDecompositionResponse decomposition,
            ProjectRecommendationResponse projectRecommendation
    ) {
        boolean zh = isZh(goal);
        Set<String> values = new LinkedHashSet<>();
        if (decomposition.subGoals() != null) {
            decomposition.subGoals().stream()
                    .limit(3)
                    .map(SubGoalResponse::title)
                    .filter(title -> title != null && !title.isBlank())
                    .map(String::trim)
                    .forEach(values::add);
        }
        cleanList(projectRecommendation.finalDeliverables(), List.of()).stream()
                .limit(2)
                .forEach(values::add);

        if (values.isEmpty()) {
            values.add(zh ? "形成第一阶段成果记录" : "Produce the first stage progress artifact");
            values.add(zh ? "完成一次路径复盘" : "Complete one path review");
        }
        return List.copyOf(values);
    }

    private List<String> riskSignals(
            Goal goal,
            SkillProfileResponse profile,
            SkillGapAnalysisResponse skillGapAnalysis
    ) {
        boolean zh = isZh(goal);
        Set<String> values = new LinkedHashSet<>();
        cleanList(profile.weaknesses(), List.of()).stream()
                .limit(3)
                .forEach(values::add);
        if (skillGapAnalysis.skillGaps() != null) {
            skillGapAnalysis.skillGaps().stream()
                    .filter(skillGap -> "high".equalsIgnoreCase(skillGap.priority()))
                    .map(skillGap -> firstPresent(skillGap.reason(), skillGap.skill()))
                    .filter(value -> value != null && !value.isBlank())
                    .limit(2)
                    .map(String::trim)
                    .forEach(values::add);
        }
        if (values.isEmpty()) {
            values.add(zh ? "如果没有稳定练习节奏，路径很容易中断。" : "The path will stall if the practice rhythm does not stay steady.");
        }
        return List.copyOf(values);
    }

    private List<String> evidence(
            Goal goal,
            SkillProfileResponse profile,
            SkillGapAnalysisResponse skillGapAnalysis,
            KnowledgeContextBundle knowledgeContext
    ) {
        boolean zh = isZh(goal);
        Set<String> values = new LinkedHashSet<>();
        cleanList(profile.currentSkills(), List.of()).stream()
                .limit(3)
                .map(skill -> zh ? "已有基础：" + skill : "Existing foundation: " + skill)
                .forEach(values::add);
        cleanList(profile.strengths(), List.of()).stream()
                .limit(2)
                .map(strength -> zh ? "已有优势：" + strength : "Observed strength: " + strength)
                .forEach(values::add);
        if (skillGapAnalysis.skillGaps() != null) {
            skillGapAnalysis.skillGaps().stream()
                    .limit(2)
                    .map(skillGap -> zh
                            ? "优先补齐：" + skillGap.skill()
                            : "Priority gap: " + skillGap.skill())
                    .forEach(values::add);
        }
        knowledgeContext.evidence().stream()
                .limit(3)
                .forEach(values::add);
        return values.isEmpty()
                ? List.of(zh ? "尚未形成足够证据，建议先完成一轮基础分析。" : "There is not enough evidence yet; start with one full analysis cycle.")
                : List.copyOf(values);
    }

    private SkillGapResponse firstSkillGap(SkillGapAnalysisResponse skillGapAnalysis) {
        if (skillGapAnalysis.skillGaps() == null || skillGapAnalysis.skillGaps().isEmpty()) {
            return null;
        }
        return skillGapAnalysis.skillGaps().get(0);
    }

    private boolean isZh(Goal goal) {
        return goal.getResponseLanguage() != null
                && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
    }

    private List<String> cleanList(List<String> values, List<String> fallback) {
        if (values == null) {
            return fallback;
        }
        List<String> cleaned = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private String joinTop(List<String> values, int limit) {
        return values.stream()
                .limit(limit)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private Integer positiveOrDefault(Integer value, Integer fallback) {
        if (value != null && value > 0) {
            return value;
        }
        return fallback != null && fallback > 0 ? fallback : 21;
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        if (fallback != null && fallback.compareTo(BigDecimal.ZERO) > 0) {
            return fallback;
        }
        return BigDecimal.valueOf(2);
    }

    private String fallbackTrackTitle(Goal goal) {
        return isZh(goal)
                ? firstPresent(goal.getTitle(), "通用成长路径") + " 学习主线"
                : firstPresent(goal.getTitle(), "General growth path") + " learning track";
    }

    private String defaultSummary(Goal goal) {
        return isZh(goal)
                ? "这条路径会把当前基础、关键差距和可验证成果串成一条更聚焦的提升主线。"
                : "This path turns the current foundation, key gaps, and visible outcomes into one focused growth track.";
    }

    private String defaultDifficulty(Goal goal) {
        return isZh(goal) ? "中等" : "medium";
    }

    private List<String> defaultFocusAreas(Goal goal) {
        return isZh(goal)
                ? List.of("关键基础", "稳定练习", "场景应用", "反馈复盘")
                : List.of("Core foundation", "Consistent practice", "Applied scenarios", "Feedback review");
    }

    private List<String> defaultDeliverables(Goal goal) {
        return isZh(goal)
                ? List.of("阶段性成果记录", "可展示的练习输出", "复盘笔记")
                : List.of("Stage progress notes", "Visible practice outputs", "Review summary");
    }
}
