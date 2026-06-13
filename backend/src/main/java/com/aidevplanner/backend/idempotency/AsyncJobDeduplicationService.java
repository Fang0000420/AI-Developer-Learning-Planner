package com.aidevplanner.backend.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AsyncJobDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobDeduplicationService.class);

    private final Duration activeJobTtl;
    private final StringRedisTemplate redisTemplate;

    public AsyncJobDeduplicationService(
            @Value("${app.redis.async-job-active-ttl:PT30M}") Duration activeJobTtl,
            StringRedisTemplate redisTemplate
    ) {
        this.activeJobTtl = activeJobTtl;
        this.redisTemplate = redisTemplate;
    }

    public Optional<UUID> getActiveJobId(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException | DataAccessException exception) {
            log.debug("Unable to read active job id for {}: {}", key, exception.getMessage());
            return Optional.empty();
        }
    }

    public void saveActiveJobId(String key, UUID jobId) {
        try {
            redisTemplate.opsForValue().set(key, jobId.toString(), activeJobTtl);
        } catch (DataAccessException exception) {
            log.debug("Unable to save active job id for {}: {}", key, exception.getMessage());
        }
    }

    public void clear(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException exception) {
            log.debug("Unable to clear active job id for {}: {}", key, exception.getMessage());
        }
    }
}
