package com.zhongyan.uav.agent.api.response;

import com.zhongyan.uav.agent.domain.AgentMessage;

import java.time.Instant;
import java.util.Map;

public record AgentMessageView(
        String messageId,
        String sessionId,
        String role,
        String content,
        String status,
        Map<String, Object> metadata,
        Instant createdAt) {
    /**
     * 将会话消息转换为 API 视图，保留元数据便于前端展示工具轨迹和模型状态。
     */
    public static AgentMessageView fromDomain(AgentMessage message) {
        return new AgentMessageView(message.messageId(), message.sessionId(), message.role().name(),
                message.content(), "SAVED", message.metadata(), message.createdAt());
    }
}
