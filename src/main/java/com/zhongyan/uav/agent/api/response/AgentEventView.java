package com.zhongyan.uav.agent.api.response;

import com.zhongyan.uav.agent.port.AgentEvent;

import java.time.Instant;
import java.util.Map;

public record AgentEventView(
        String eventId,
        String sessionId,
        String eventType,
        Map<String, Object> payload,
        Instant occurredAt) {
    public AgentEventView {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static AgentEventView fromEvent(AgentEvent event) {
        return new AgentEventView(event.eventId(), event.sessionId(), event.eventType(),
                event.payload(), event.occurredAt());
    }
}
