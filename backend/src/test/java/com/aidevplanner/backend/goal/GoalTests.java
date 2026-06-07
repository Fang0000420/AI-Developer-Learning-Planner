package com.aidevplanner.backend.goal;

import com.aidevplanner.backend.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoalTests {

    @Test
    void defaultsGoalStatusToActive() {
        User user = new User("alice", "alice@example.com", "hashed-password");
        Goal goal = new Goal(user, "Build an AI planner", 21);

        goal.onCreate();

        assertThat(goal.getUser()).isSameAs(user);
        assertThat(goal.getTitle()).isEqualTo("Build an AI planner");
        assertThat(goal.getDurationDays()).isEqualTo(21);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.getUpdatedAt()).isNotNull();
    }
}
