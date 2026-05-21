package com.zhongyan.uav.agent.port;

import java.util.Map;

public record AgentStreamChunk(
        String sessionId,
        String content,
        boolean done,
        Map<String, Object> metadata) {
    public AgentStreamChunk {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
