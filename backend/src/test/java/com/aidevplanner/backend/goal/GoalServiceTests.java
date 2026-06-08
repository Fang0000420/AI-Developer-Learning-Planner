package com.aidevplanner.backend.goal;

import com.aidevplanner.backend.common.ResourceNotFoundException;
import com.aidevplanner.backend.user.User;
import com.aidevplanner.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTests {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoalService goalService;

    @Test
    void createsGoalForDefaultUserWhenUserIdIsMissing() {
        User user = new User("demo-user", "demo@example.com", "not-used");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByUsername("demo-user")).thenReturn(Optional.of(user));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal savedGoal = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedGoal, "id", 10L);
            return savedGoal;
        });

        GoalResponse response = goalService.createGoal(new GoalCreateRequest(
                null,
                "  Build an AI Agent project  ",
                "  Practice production workflows.  ",
                21,
                new BigDecimal("2.0")
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Build an AI Agent project");
        assertThat(response.description()).isEqualTo("Practice production workflows.");
        assertThat(response.dailyAvailableHours()).isEqualByComparingTo("2.0");
        assertThat(user.getDailyAvailableHours()).isEqualByComparingTo("2.0");
    }

    @Test
    void updatesGoalFieldsAndStatus() {
        User user = new User("alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", 1L);
        Goal goal = new Goal(user, "Old title", 14);
        ReflectionTestUtils.setField(goal, "id", 10L);

        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));

        GoalResponse response = goalService.updateGoal(10L, new GoalUpdateRequest(
                "New title",
                null,
                21,
                GoalStatus.PAUSED,
                new BigDecimal("3.0")
        ));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isNull();
        assertThat(response.durationDays()).isEqualTo(21);
        assertThat(response.status()).isEqualTo(GoalStatus.PAUSED);
        assertThat(response.dailyAvailableHours()).isEqualByComparingTo("3.0");
    }

    @Test
    void deletesExistingGoal() {
        when(goalRepository.existsById(10L)).thenReturn(true);

        goalService.deleteGoal(10L);

        verify(goalRepository).deleteById(10L);
    }

    @Test
    void throwsWhenDeletingMissingGoal() {
        when(goalRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> goalService.deleteGoal(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Goal 404 was not found.");
    }

    @Test
    void createsDefaultUserWhenMissing() {
        when(userRepository.findByUsername("demo-user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            return savedUser;
        });
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        goalService.createGoal(new GoalCreateRequest(
                null,
                "Build an AI Agent project",
                null,
                14,
                null
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("demo-user");
    }
}
