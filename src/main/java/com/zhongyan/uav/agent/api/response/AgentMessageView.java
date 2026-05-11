package com.zhongyan.uav.agent.api.response;

import java.time.Instant;

public record AgentMessageView(
        String messageId,
        String sessionId,
        String role,
        String content,
        String status,
        Instant createdAt) {
    /**
     * 构建 Agent 消息占位视图，避免 API 空壳调用模型服务。
     */
    public static AgentMessageView placeholder(String messageId, String sessionId, String role, String content) {
        return new AgentMessageView(messageId, sessionId, role, content, "PLACEHOLDER", Instant.now());
    }
}
