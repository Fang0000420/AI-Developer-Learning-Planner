package com.aidevplanner.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileVersionRepository extends JpaRepository<UserProfileVersion, Long> {

    List<UserProfileVersion> findTop5ByUserIdOrderByVersionDesc(Long userId);

    Optional<UserProfileVersion> findFirstByUserIdOrderByVersionDesc(Long userId);
}
