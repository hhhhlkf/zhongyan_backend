package com.zhongyan.uav.agent.port;

import java.util.Map;

public record AgentToolResult(
        boolean success,
        String message,
        Map<String, Object> output) {
    public AgentToolResult {
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static AgentToolResult success(Map<String, Object> output) {
        return new AgentToolResult(true, "OK", output);
    }

    public static AgentToolResult blocked(String message) {
        return new AgentToolResult(false, message, Map.of());
    }
}
