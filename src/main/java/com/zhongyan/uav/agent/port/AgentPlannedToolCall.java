package com.zhongyan.uav.agent.port;

import java.util.Map;

public record AgentPlannedToolCall(
        String toolName,
        Map<String, Object> input) {
    public AgentPlannedToolCall {
        input = input == null ? Map.of() : Map.copyOf(input);
    }
}
