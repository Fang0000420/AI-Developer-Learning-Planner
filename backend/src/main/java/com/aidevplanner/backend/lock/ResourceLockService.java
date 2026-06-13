package com.aidevplanner.backend.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class ResourceLockService {

    private static final Logger log = LoggerFactory.getLogger(ResourceLockService.class);

    private final Duration lockTtl;
    private final StringRedisTemplate redisTemplate;

    public ResourceLockService(
            @Value("${app.redis.resource-lock-ttl:PT30M}") Duration lockTtl,
            StringRedisTemplate redisTemplate
    ) {
        this.lockTtl = lockTtl;
        this.redisTemplate = redisTemplate;
    }

    public ResourceLockHandle tryAcquire(String key) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, lockTtl);
            if (Boolean.TRUE.equals(acquired)) {
                return new ResourceLockHandle(key, token, true);
            }
            return null;
        } catch (DataAccessException exception) {
            log.debug("Redis lock unavailable for {}: {}", key, exception.getMessage());
            return ResourceLockHandle.bypass(key, token);
        }
    }

    public void release(ResourceLockHandle handle) {
        if (handle == null || !handle.locked()) {
            return;
        }
        try {
            String currentToken = redisTemplate.opsForValue().get(handle.key());
            if (handle.token().equals(currentToken)) {
                redisTemplate.delete(handle.key());
            }
        } catch (DataAccessException exception) {
            log.debug("Unable to release Redis lock {}: {}", handle.key(), exception.getMessage());
        }
    }
}
