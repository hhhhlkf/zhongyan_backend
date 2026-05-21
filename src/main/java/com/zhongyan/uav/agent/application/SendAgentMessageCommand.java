package com.zhongyan.uav.agent.application;

import java.util.Map;

public record SendAgentMessageCommand(
        String sessionId,
        String userId,
        String content,
        Map<String, Object> metadata) {
    public SendAgentMessageCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
