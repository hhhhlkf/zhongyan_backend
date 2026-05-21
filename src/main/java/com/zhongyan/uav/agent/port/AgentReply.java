package com.zhongyan.uav.agent.port;

import java.util.List;
import java.util.Map;

public record AgentReply(
        String content,
        List<AgentPlannedToolCall> toolCalls,
        Map<String, Object> metadata) {
    public AgentReply {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
