package com.zhongyan.uav.agent.api.response;

import java.time.Instant;

public record AgentSessionView(
        String sessionId,
        String missionId,
        String taskId,
        String status,
        Instant createdAt) {
    /**
     * 构建 Agent 会话占位视图，避免 API 空壳调用模型编排。
     */
    public static AgentSessionView placeholder(String sessionId, String missionId, String taskId) {
        return new AgentSessionView(sessionId, missionId, taskId, "PLACEHOLDER", Instant.now());
    }
}
