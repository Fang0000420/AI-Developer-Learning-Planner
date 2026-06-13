package com.aidevplanner.backend.goal;

import com.aidevplanner.backend.auth.AuthenticatedUserService;
import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.user.User;
import com.aidevplanner.backend.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {

    private static final String DEFAULT_USERNAME = "demo-user";
    private static final String DEFAULT_EMAIL = "demo@example.com";
    private static final String DEFAULT_PASSWORD_HASH = "not-used";

    private final GoalRepository goalRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final GoalKnowledgePreferenceService goalKnowledgePreferenceService;
    private final UserRepository userRepository;

    public GoalService(
            GoalRepository goalRepository,
            AuthenticatedUserService authenticatedUserService,
            GoalKnowledgePreferenceService goalKnowledgePreferenceService,
            UserRepository userRepository
    ) {
        this.goalRepository = goalRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.goalKnowledgePreferenceService = goalKnowledgePreferenceService;
        this.userRepository = userRepository;
    }

    @Transactional
    public GoalResponse createGoal(GoalCreateRequest request) {
        User user = resolveUser(request.userId());
        updateDailyAvailableHours(user, request.dailyAvailableHours());
        updateBackground(user, request.technicalBackground());

        Goal goal = new Goal(user, normalizeRequired(request.title()), request.durationDays());
        goal.setDescription(normalizeOptional(request.description()));
        goal.setResponseLanguage(defaultLanguage(request.responseLanguage()));

        return GoalResponse.from(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(Long userId, GoalStatus status) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        if (currentUserId != null) {
            userId = currentUserId;
        }

        List<Goal> goals;
        if (userId != null && status != null) {
            goals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else if (userId != null) {
            goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if (status != null) {
            goals = goalRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            goals = goalRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return goals.stream()
                .map(GoalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoal(Long goalId) {
        return GoalResponse.from(findGoal(goalId));
    }

    @Transactional
    public GoalResponse updateGoal(Long goalId, GoalUpdateRequest request) {
        Goal goal = findGoal(goalId);
        goal.setTitle(normalizeRequired(request.title()));
        goal.setDescription(normalizeOptional(request.description()));
        goal.setDurationDays(request.durationDays());
        goal.setResponseLanguage(defaultLanguage(request.responseLanguage()));
        if (request.status() != null) {
            goal.setStatus(request.status());
        }
        updateDailyAvailableHours(goal.getUser(), request.dailyAvailableHours());

        return GoalResponse.from(goal);
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new ResourceNotFoundException("Goal", goalId);
        }
        goalRepository.deleteById(goalId);
    }

    @Transactional(readOnly = true)
    public GoalKnowledgePreferenceResponse getKnowledgePreference(Long goalId) {
        return goalKnowledgePreferenceService.response(findGoal(goalId));
    }

    @Transactional
    public GoalKnowledgePreferenceResponse updateKnowledgePreference(
            Long goalId,
            GoalKnowledgePreferenceRequest request
    ) {
        Goal goal = findGoal(goalId);
        goalKnowledgePreferenceService.write(goal, request);
        return goalKnowledgePreferenceService.response(goal);
    }

    private Goal findGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        authenticatedUserService.currentUserId().ifPresent(currentUserId -> {
            if (!goal.getUser().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("Goal", goalId);
            }
        });
        return goal;
    }

    private User resolveUser(Long userId) {
        Long currentUserId = authenticatedUserService.currentUserId().orElse(null);
        if (currentUserId != null) {
            if (userId != null && !userId.equals(currentUserId)) {
                throw new ResourceNotFoundException("User", userId);
            }
            return userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
        }

        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        }

        return userRepository.findByUsername(DEFAULT_USERNAME)
                .orElseGet(() -> userRepository.save(new User(
                        DEFAULT_USERNAME,
                        DEFAULT_EMAIL,
                        DEFAULT_PASSWORD_HASH
                )));
    }

    private void updateDailyAvailableHours(User user, BigDecimal dailyAvailableHours) {
        if (dailyAvailableHours != null) {
            user.setDailyAvailableHours(dailyAvailableHours);
        }
    }

    private void updateBackground(User user, String background) {
        String normalizedBackground = normalizeOptional(background);
        if (normalizedBackground != null) {
            user.setBackground(normalizedBackground);
        }
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseLanguage defaultLanguage(ResponseLanguage responseLanguage) {
        return responseLanguage == null ? ResponseLanguage.zh : responseLanguage;
    }
}
