package com.aidevplanner.backend.cache;

import com.aidevplanner.backend.asyncjob.AsyncJobResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AsyncJobCacheService {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobCacheService.class);

    private final Duration cacheTtl;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public AsyncJobCacheService(
            @Value("${app.redis.async-job-cache-ttl:PT24H}") Duration cacheTtl,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate
    ) {
        this.cacheTtl = cacheTtl;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public Optional<AsyncJobResponse> get(UUID jobId) {
        try {
            String payload = redisTemplate.opsForValue().get(cacheKey(jobId));
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, AsyncJobResponse.class));
        } catch (JsonProcessingException | DataAccessException exception) {
            log.debug("Unable to read async job {} from Redis cache: {}", jobId, exception.getMessage());
            return Optional.empty();
        }
    }

    public void put(AsyncJobResponse response) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey(response.jobId()),
                    objectMapper.writeValueAsString(response),
                    cacheTtl
            );
        } catch (JsonProcessingException | DataAccessException exception) {
            log.debug(
                    "Unable to store async job {} in Redis cache: {}",
                    response.jobId(),
                    exception.getMessage()
            );
        }
    }

    public void evict(UUID jobId) {
        try {
            redisTemplate.delete(cacheKey(jobId));
        } catch (DataAccessException exception) {
            log.debug("Unable to evict async job {} from Redis cache: {}", jobId, exception.getMessage());
        }
    }

    private String cacheKey(UUID jobId) {
        return "async-job:response:" + jobId;
    }
}
