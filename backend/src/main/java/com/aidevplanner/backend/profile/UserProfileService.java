package com.aidevplanner.backend.profile;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.progress.ProgressReviewAgentResponse;
import com.aidevplanner.backend.progress.ProgressSubmitRequest;
import com.aidevplanner.backend.user.User;
import com.aidevplanner.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserProfileService {

    private final AuthenticatedUserService authenticatedUserService;
    private final GoalProfileSnapshotRepository goalProfileSnapshotRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileVersionRepository userProfileVersionRepository;
    private final UserRepository userRepository;

    public UserProfileService(
            AuthenticatedUserService authenticatedUserService,
            GoalProfileSnapshotRepository goalProfileSnapshotRepository,
            UserProfileRepository userProfileRepository,
            UserProfileVersionRepository userProfileVersionRepository,
            UserRepository userRepository
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.goalProfileSnapshotRepository = goalProfileSnapshotRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileVersionRepository = userProfileVersionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = currentUser();
        return userProfileRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request) {
        User user = currentUser();
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile", user.getId()));

        UserProfileVersion currentVersion = userProfileVersionRepository.findFirstByUserIdOrderByVersionDesc(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile version", user.getId()));
        UserProfileVersion nextVersion = userProfileVersionRepository.save(new UserProfileVersion(
                profile,
                user,
                null,
                currentVersion.getVersion() + 1,
                profile.getProfileSummary(),
                firstPresent(request.preferredLearningStyle(), profile.getPreferredLearningStyle()),
                firstPresent(request.pacePreference(), profile.getPacePreference()),
                firstPresent(request.timeBudgetNote(), profile.getTimeBudgetNote()),
                firstPresent(request.manualCorrection(), profile.getManualCorrection()),
                profile.getCurrentSkills(),
                profile.getStrengths(),
                profile.getWeaknesses(),
                profile.getFocusAreas(),
                profile.getRiskSignals(),
                appendCorrectionEvidence(profile.getEvidence(), request.manualCorrection(), profile.getUser()),
                profile.getRecommendedDirection()
        ));
        profile.updateFromVersion(nextVersion);
        UserProfile savedProfile = userProfileRepository.save(profile);
        return toResponse(savedProfile);
    }

    @Transactional
    public UserProfileVersion recordGoalAnalysis(Goal goal, SkillProfile skillProfile, ProfileAnalyzeResponse response) {
        UserProfile profile = userProfileRepository.findByUserId(goal.getUser().getId())
                .orElseGet(() -> createInitialProfile(goal.getUser(), response, goal));

        int nextVersionNumber = profile.getCurrentVersion() + 1;
        UserProfileVersion version = userProfileVersionRepository.save(new UserProfileVersion(
                profile,
                goal.getUser(),
                goal,
                nextVersionNumber,
                buildSummary(goal, response, profile.getManualCorrection()),
                defaultLearningStyle(goal.getUser()),
                defaultPacePreference(goal.getUser()),
                timeBudgetNote(goal.getUser()),
                profile.getManualCorrection(),
                copyList(response.currentSkills()),
                copyList(response.strengths()),
                copyList(response.weaknesses()),
                focusAreas(response, goal),
                riskSignals(response, goal),
                evidence(response, goal),
                firstPresent(response.recommendedDirection(), fallbackDirection(goal))
        ));
        profile.updateFromVersion(version);
        userProfileRepository.save(profile);

        goalProfileSnapshotRepository.save(new GoalProfileSnapshot(
                goal.getUser(),
                goal,
                skillProfile,
                version,
                buildGoalSnapshotSummary(goal, response),
                version.getPreferredLearningStyle(),
                version.getPacePreference(),
                version.getTimeBudgetNote(),
                version.getCurrentSkills(),
                version.getStrengths(),
                version.getWeaknesses(),
                version.getFocusAreas(),
                version.getRiskSignals(),
                version.getEvidence(),
                version.getRecommendedDirection()
        ));
        return version;
    }

    @Transactional
    public UserProfileResponse applyProgressFeedback(
            LearningPlan plan,
            ProgressReviewAgentResponse review,
            ProgressSubmitRequest request
    ) {
        UserProfile profile = userProfileRepository.findByUserId(plan.getUser().getId())
                .orElse(null);
        if (profile == null) {
            return null;
        }

        UserProfileVersion currentVersion = userProfileVersionRepository.findFirstByUserIdOrderByVersionDesc(plan.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile version", plan.getUser().getId()));

        String updatedPacePreference = applyPaceAdjustment(currentVersion.getPacePreference(), review);
        String updatedTimeBudgetNote = applyTimeBudgetFeedback(currentVersion.getTimeBudgetNote(), review, request, plan);
        List<String> updatedRiskSignals = mergeRiskSignals(currentVersion.getRiskSignals(), review);
        List<String> updatedEvidence = mergeFeedbackEvidence(currentVersion.getEvidence(), review, request);
        List<String> updatedFocusAreas = mergeFocusAreas(currentVersion.getFocusAreas(), review);

        UserProfileVersion nextVersion = userProfileVersionRepository.save(new UserProfileVersion(
                profile,
                plan.getUser(),
                plan.getGoal(),
                currentVersion.getVersion() + 1,
                buildFeedbackSummary(plan, review),
                currentVersion.getPreferredLearningStyle(),
                updatedPacePreference,
                updatedTimeBudgetNote,
                currentVersion.getManualCorrection(),
                currentVersion.getCurrentSkills(),
                currentVersion.getStrengths(),
                currentVersion.getWeaknesses(),
                updatedFocusAreas,
                updatedRiskSignals,
                updatedEvidence,
                currentVersion.getRecommendedDirection()
        ));
        profile.updateFromVersion(nextVersion);
        UserProfile savedProfile = userProfileRepository.save(profile);

        goalProfileSnapshotRepository.save(new GoalProfileSnapshot(
                plan.getUser(),
                plan.getGoal(),
                null,
                nextVersion,
                buildFeedbackGoalSnapshotSummary(plan, review),
                nextVersion.getPreferredLearningStyle(),
                nextVersion.getPacePreference(),
                nextVersion.getTimeBudgetNote(),
                nextVersion.getCurrentSkills(),
                nextVersion.getStrengths(),
                nextVersion.getWeaknesses(),
                nextVersion.getFocusAreas(),
                nextVersion.getRiskSignals(),
                nextVersion.getEvidence(),
                nextVersion.getRecommendedDirection()
        ));
        return toResponse(savedProfile);
    }

    private UserProfile createInitialProfile(User user, ProfileAnalyzeResponse response, Goal goal) {
        return userProfileRepository.save(new UserProfile(
                user,
                0,
                buildSummary(goal, response, null),
                defaultLearningStyle(user),
                defaultPacePreference(user),
                timeBudgetNote(user),
                null,
                copyList(response.currentSkills()),
                copyList(response.strengths()),
                copyList(response.weaknesses()),
                focusAreas(response, goal),
                riskSignals(response, goal),
                evidence(response, goal),
                firstPresent(response.recommendedDirection(), fallbackDirection(goal))
        ));
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        List<GoalProfileSnapshotResponse> snapshots = goalProfileSnapshotRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(profile.getUser().getId())
                .stream()
                .map(GoalProfileSnapshotResponse::from)
                .toList();
        return UserProfileResponse.from(profile, snapshots);
    }

    private User currentUser() {
        Long currentUserId = authenticatedUserService.currentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User", "current"));
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
    }

    private String buildSummary(Goal goal, ProfileAnalyzeResponse response, String manualCorrection) {
        boolean zh = goal.getResponseLanguage() != null && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
        String strengths = joinTop(copyList(response.strengths()), 2);
        String weaknesses = joinTop(copyList(response.weaknesses()), 2);
        String correctionText = manualCorrection == null || manualCorrection.isBlank()
                ? ""
                : zh ? " 当前用户补充说明：" + manualCorrection.trim() : " User correction: " + manualCorrection.trim();
        if (zh) {
            return "当前长期画像显示，这位用户的主要优势在 "
                    + fallbackText(strengths, "既有基础与执行意愿")
                    + "，需要优先补齐 "
                    + fallbackText(weaknesses, "关键能力短板")
                    + "，并围绕“" + goal.getTitle() + "”建立持续输出。"
                    + correctionText;
        }
        return "The long-term profile shows strengths in "
                + fallbackText(strengths, "existing foundation and willingness to execute")
                + ", while the next priority is to strengthen "
                + fallbackText(weaknesses, "key capability gaps")
                + " and build consistent outputs around " + goal.getTitle() + "."
                + correctionText;
    }

    private String buildFeedbackSummary(LearningPlan plan, ProgressReviewAgentResponse review) {
        boolean zh = isZh(plan.getGoal());
        String wins = joinTop(copyList(review.wins()), 2);
        String nextFocus = joinTop(copyList(review.nextFocus()), 2);
        if (zh) {
            return "根据第 " + plan.getId() + " 号计划最近一次反馈，当前阶段的正向进展体现在 "
                    + fallbackText(wins, "已有任务推进")
                    + "；下一步需要继续聚焦 "
                    + fallbackText(nextFocus, "清理阻塞并稳住节奏")
                    + "。";
        }
        return "The latest feedback on plan " + plan.getId()
                + " shows positive progress in "
                + fallbackText(wins, "recent task execution")
                + ", and the next focus should stay on "
                + fallbackText(nextFocus, "clearing blockers and stabilizing pace")
                + ".";
    }

    private String buildFeedbackGoalSnapshotSummary(LearningPlan plan, ProgressReviewAgentResponse review) {
        boolean zh = isZh(plan.getGoal());
        if (zh) {
            return "这份快照来自日进度反馈，反映了“" + plan.getGoal().getTitle()
                    + "”在执行阶段的节奏变化、重点转移和风险更新。";
        }
        return "This snapshot comes from daily progress feedback and captures pace shifts, focus changes, and updated risks for "
                + plan.getGoal().getTitle() + ".";
    }

    private String buildGoalSnapshotSummary(Goal goal, ProfileAnalyzeResponse response) {
        boolean zh = goal.getResponseLanguage() != null && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
        if (zh) {
            return "这次快照围绕“" + goal.getTitle() + "”生成，用于记录当前阶段的能力判断和推荐方向。";
        }
        return "This snapshot is tied to " + goal.getTitle() + " and records the current capability judgment and recommended direction.";
    }

    private String defaultLearningStyle(User user) {
        BigDecimal hours = user.getDailyAvailableHours();
        if (hours != null && hours.compareTo(BigDecimal.valueOf(2.5)) >= 0) {
            return "项目驱动";
        }
        return "小步快跑";
    }

    private String defaultPacePreference(User user) {
        BigDecimal hours = user.getDailyAvailableHours();
        if (hours == null) {
            return "稳步推进";
        }
        if (hours.compareTo(BigDecimal.valueOf(1.5)) < 0) {
            return "轻量节奏";
        }
        if (hours.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return "高投入节奏";
        }
        return "稳步推进";
    }

    private String timeBudgetNote(User user) {
        BigDecimal hours = user.getDailyAvailableHours();
        if (hours == null) {
            return "每日时间预算尚未明确。";
        }
        return "当前可投入时间约为每日 " + hours + " 小时。";
    }

    private String applyPaceAdjustment(String currentValue, ProgressReviewAgentResponse review) {
        String base = firstPresent(currentValue, "稳步推进");
        return switch (firstPresent(review.paceAdjustment(), "keep")) {
            case "slower" -> "需要放慢节奏";
            case "faster" -> "可以加快节奏";
            default -> base;
        };
    }

    private String applyTimeBudgetFeedback(
            String currentValue,
            ProgressReviewAgentResponse review,
            ProgressSubmitRequest request,
            LearningPlan plan
    ) {
        String base = firstPresent(currentValue, timeBudgetNote(plan.getUser()));
        int blockerCount = request.blockers() == null ? 0 : (int) request.blockers().stream()
                .filter(item -> item != null && !item.isBlank())
                .count();
        if ("slower".equalsIgnoreCase(review.paceAdjustment())) {
            return base + " 最近反馈显示需要预留更多缓冲时间。";
        }
        if ("faster".equalsIgnoreCase(review.paceAdjustment()) && blockerCount == 0) {
            return base + " 最近反馈显示当前时间预算还有余量。";
        }
        return base;
    }

    private List<String> focusAreas(ProfileAnalyzeResponse response, Goal goal) {
        Set<String> values = new LinkedHashSet<>(copyList(response.weaknesses()));
        if (values.isEmpty()) {
            values.add(goal.getTitle());
        }
        values.addAll(copyList(response.currentSkills()).stream().limit(2).toList());
        return List.copyOf(values).stream().limit(4).toList();
    }

    private List<String> mergeFocusAreas(List<String> currentValues, ProgressReviewAgentResponse review) {
        Set<String> values = new LinkedHashSet<>(currentValues);
        values.addAll(copyList(review.nextFocus()));
        return List.copyOf(values).stream().limit(5).toList();
    }

    private List<String> riskSignals(ProfileAnalyzeResponse response, Goal goal) {
        Set<String> values = new LinkedHashSet<>();
        copyList(response.weaknesses()).stream()
                .limit(3)
                .map(item -> "需要关注：" + item)
                .forEach(values::add);
        if (values.isEmpty()) {
            values.add("如果缺少稳定练习节奏，" + goal.getTitle() + " 这条路径容易中断。");
        }
        return List.copyOf(values);
    }

    private List<String> mergeRiskSignals(List<String> currentValues, ProgressReviewAgentResponse review) {
        Set<String> values = new LinkedHashSet<>(currentValues);
        if ("major".equalsIgnoreCase(review.impact())) {
            values.add("最近一次反馈显示当前任务范围过大，需要立即收缩。");
        } else if ("medium".equalsIgnoreCase(review.impact())) {
            values.add("最近一次反馈显示当前节奏存在波动，需要先处理阻塞。");
        }
        copyList(review.blockers()).stream()
                .limit(2)
                .map(item -> "最新阻塞：" + item)
                .forEach(values::add);
        return List.copyOf(values).stream().limit(5).toList();
    }

    private List<String> evidence(ProfileAnalyzeResponse response, Goal goal) {
        Set<String> values = new LinkedHashSet<>();
        copyList(response.currentSkills()).stream()
                .limit(3)
                .map(item -> "已有基础：" + item)
                .forEach(values::add);
        copyList(response.strengths()).stream()
                .limit(2)
                .map(item -> "已有优势：" + item)
                .forEach(values::add);
        if (values.isEmpty()) {
            values.add("当前证据主要来自目标“" + goal.getTitle() + "”的画像分析。");
        }
        return List.copyOf(values);
    }

    private List<String> mergeFeedbackEvidence(
            List<String> currentValues,
            ProgressReviewAgentResponse review,
            ProgressSubmitRequest request
    ) {
        Set<String> values = new LinkedHashSet<>(currentValues);
        copyList(review.wins()).stream()
                .limit(2)
                .map(item -> "最近进展：" + item)
                .forEach(values::add);
        if (request.userFeedback() != null && !request.userFeedback().isBlank()) {
            values.add("用户反馈：" + request.userFeedback().trim());
        }
        values.add("复盘信心：" + firstPresent(review.confidence(), "medium"));
        return List.copyOf(values).stream().limit(6).toList();
    }

    private List<String> appendCorrectionEvidence(List<String> evidence, String manualCorrection, User user) {
        if (manualCorrection == null || manualCorrection.isBlank()) {
            return evidence;
        }
        Set<String> values = new LinkedHashSet<>(evidence);
        values.add("用户纠偏：" + manualCorrection.trim());
        if (user.getBackground() != null && !user.getBackground().isBlank()) {
            values.add("背景信息：" + user.getBackground().trim());
        }
        return List.copyOf(values);
    }

    private boolean isZh(Goal goal) {
        return goal != null && goal.getResponseLanguage() != null
                && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
    }

    private List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
    }

    private String joinTop(List<String> values, int limit) {
        return values.stream().limit(limit).reduce((left, right) -> left + "、" + right).orElse("");
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

    private String fallbackDirection(Goal goal) {
        return "围绕“" + goal.getTitle() + "”持续建立可验证成果。";
    }
}
