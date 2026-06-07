package com.aidevplanner.backend.user;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UserTests {

    @Test
    void createsUserWithProfileFields() {
        User user = new User("alice", "alice@example.com", "hashed-password");

        user.setBackground("Java backend developer");
        user.setDailyAvailableHours(new BigDecimal("2.5"));
        user.onCreate();

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getBackground()).isEqualTo("Java backend developer");
        assertThat(user.getDailyAvailableHours()).isEqualByComparingTo("2.5");
        assertThat(user.getUpdatedAt()).isNotNull();
    }
}
