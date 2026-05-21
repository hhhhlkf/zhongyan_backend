package com.zhongyan.uav.agent.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "bms.agent")
public record AgentProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        int maxToolDepth,
        boolean streamEnabled,
        boolean approvalRequiredForWrite,
        String eventsTopic,
        Duration connectTimeout,
        Duration readTimeout,
        Redis redis) {
    public AgentProperties {
        if (enabled) {
            requireText(baseUrl, "base-url");
            requireText(chatModel, "chat-model");
            requireText(embeddingModel, "embedding-model");
        }
        apiKey = apiKey == null ? "" : apiKey;
        maxToolDepth = maxToolDepth <= 0 ? 5 : maxToolDepth;
        eventsTopic = defaultText(eventsTopic, "agent-events");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
        redis = redis == null ? Redis.defaults() : redis;
    }

    public String normalizedBaseUrl() {
        String value = defaultText(baseUrl, "http://localhost:11434/v1");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("bms.agent." + name + " must not be blank when Agent is enabled");
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record Redis(
            boolean enabled,
            int toolRateLimitPerMinute,
            Duration rateLimitWindow,
            Duration idempotencyTtl) {
        public Redis {
            toolRateLimitPerMinute = Math.max(toolRateLimitPerMinute, 0);
            rateLimitWindow = rateLimitWindow == null ? Duration.ofMinutes(1) : rateLimitWindow;
            idempotencyTtl = idempotencyTtl == null ? Duration.ofMinutes(30) : idempotencyTtl;
        }

        private static Redis defaults() {
            return new Redis(false, 60, Duration.ofMinutes(1), Duration.ofMinutes(30));
        }
    }
}
