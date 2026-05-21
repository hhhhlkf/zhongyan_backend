package com.zhongyan.uav.agent.port;

import java.time.Instant;
import java.util.Map;

/**
 * Agent 模块内部的审计事件模型。
 * <p>
 * 该事件由会话编排和 Tool Gateway 产生，随后会被转换为统一事件总线的
 * {@code AGENT_EVENT} 并推送到 {@code agent-events} 主题和实时 SSE 通道。
 */
public record AgentEvent(
        String eventId,
        String sessionId,
        String eventType,
        Map<String, Object> payload,
        Instant occurredAt) {
    public AgentEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
