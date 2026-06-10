package com.aidevplanner.backend.asyncjob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    Optional<AsyncJob> findByIdAndUserId(UUID id, Long userId);
}
