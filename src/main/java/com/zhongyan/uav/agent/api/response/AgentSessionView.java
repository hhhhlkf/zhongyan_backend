package com.zhongyan.uav.agent.api.response;

import com.zhongyan.uav.agent.domain.AgentSession;

import java.time.Instant;

public record AgentSessionView(
        String sessionId,
        String missionId,
        String taskId,
        String userId,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * 将会话领域对象转换为 API 视图，避免 Controller 暴露领域模型。
     */
    public static AgentSessionView fromDomain(AgentSession session) {
        return new AgentSessionView(session.sessionId(), session.missionId(), session.taskId(),
                session.userId(), session.title(), session.status(), session.createdAt(), session.updatedAt());
    }
}
