package com.zhongyan.uav.agent.domain;

import java.time.Instant;
import java.util.Objects;

public class AgentSession {
    private final String sessionId;
    private final String missionId;
    private final String taskId;
    private final String userId;
    private final String title;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AgentSession(String sessionId, String missionId, String taskId, String userId, String title,
                        String status, Instant createdAt, Instant updatedAt) {
        this.sessionId = requireText(sessionId, "sessionId");
        this.missionId = missionId;
        this.taskId = taskId;
        this.userId = requireText(userId, "userId");
        this.title = title;
        this.status = requireText(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static AgentSession create(String sessionId, String missionId, String taskId, String userId,
                                      String title, Instant now) {
        return new AgentSession(sessionId, missionId, taskId, userId, title, "ACTIVE", now, now);
    }

    public String sessionId() {
        return sessionId;
    }

    public String missionId() {
        return missionId;
    }

    public String taskId() {
        return taskId;
    }

    public String userId() {
        return userId;
    }

    public String title() {
        return title;
    }

    public String status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
