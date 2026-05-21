package com.zhongyan.uav.agent.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class AgentMessage {
    private final String messageId;
    private final String sessionId;
    private final AgentMessageRole role;
    private final String content;
    private final Map<String, Object> metadata;
    private final Instant createdAt;

    public AgentMessage(String messageId, String sessionId, AgentMessageRole role, String content,
                        Map<String, Object> metadata, Instant createdAt) {
        this.messageId = requireText(messageId, "messageId");
        this.sessionId = requireText(sessionId, "sessionId");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.content = requireText(content, "content");
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String messageId() {
        return messageId;
    }

    public String sessionId() {
        return sessionId;
    }

    public AgentMessageRole role() {
        return role;
    }

    public String content() {
        return content;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
