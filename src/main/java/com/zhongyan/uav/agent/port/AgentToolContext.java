package com.zhongyan.uav.agent.port;

import java.util.Map;
import java.util.Set;

public record AgentToolContext(
        String sessionId,
        String userId,
        String requestedBy,
        Set<String> permissions,
        Map<String, Object> attributes) {
    public AgentToolContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
