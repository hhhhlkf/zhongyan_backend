package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.port.AgentRuntimeGuard;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class RedisAgentRuntimeGuard implements AgentRuntimeGuard {
    private final StringRedisTemplate redisTemplate;
    private final AgentProperties properties;

    public RedisAgentRuntimeGuard(StringRedisTemplate redisTemplate, AgentProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void assertToolCallAllowed(String sessionId, String userId, String toolName) {
        AgentProperties.Redis redis = properties.redis();
        if (!redis.enabled() || redis.toolRateLimitPerMinute() <= 0) {
            return;
        }
        String key = "bms:agent:rate:" + safe(userId, sessionId) + ":" + toolName;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, redis.rateLimitWindow());
        }
        if (count != null && count > redis.toolRateLimitPerMinute()) {
            throw new IllegalStateException("Agent tool rate limit exceeded for " + toolName);
        }
    }

    @Override
    public Optional<String> reserveIdempotencyKey(String sessionId, String toolName, Map<String, Object> input,
                                                  String toolCallId) {
        AgentProperties.Redis redis = properties.redis();
        String idempotencyKey = idempotencyKey(input);
        if (!redis.enabled() || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String key = "bms:agent:idempotency:" + sessionId + ":" + toolName + ":" + idempotencyKey;
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, toolCallId, redis.idempotencyTtl());
        if (Boolean.TRUE.equals(reserved)) {
            return Optional.empty();
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void releaseIdempotencyKey(String sessionId, String toolName, Map<String, Object> input) {
        String idempotencyKey = idempotencyKey(input);
        if (idempotencyKey.isBlank()) {
            return;
        }
        redisTemplate.delete("bms:agent:idempotency:" + sessionId + ":" + toolName + ":" + idempotencyKey);
    }

    private String idempotencyKey(Map<String, Object> input) {
        Object value = input == null ? null : input.get("idempotencyKey");
        return value == null ? "" : String.valueOf(value);
    }

    private String safe(String primary, String fallback) {
        String value = primary == null || primary.isBlank() ? fallback : primary;
        return value == null || value.isBlank() ? "anonymous" : value.replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
