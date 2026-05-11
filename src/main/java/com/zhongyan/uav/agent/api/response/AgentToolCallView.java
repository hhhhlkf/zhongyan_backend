package com.zhongyan.uav.agent.api.response;

import java.time.Instant;

public record AgentToolCallView(
        String toolCallId,
        String sessionId,
        String toolName,
        String riskLevel,
        String status,
        Instant updatedAt) {
    /**
     * 构建 Agent 工具调用占位视图，避免 API 空壳绕过 Tool Gateway。
     */
    public static AgentToolCallView placeholder(String toolCallId, String sessionId, String status) {
        return new AgentToolCallView(toolCallId, sessionId, null, "UNKNOWN", status, Instant.now());
    }
}
