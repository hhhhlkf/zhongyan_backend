package com.zhongyan.uav.agent.application;

import java.util.Map;
import java.util.Set;

public record InvokeAgentToolCommand(
        String sessionId,
        String messageId,
        String userId,
        String toolName,
        Map<String, Object> input,
        Set<String> permissions) {
    public InvokeAgentToolCommand {
        input = input == null ? Map.of() : Map.copyOf(input);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
