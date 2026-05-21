package com.zhongyan.uav.agent.application;

import java.util.Map;

public record CreateAgentSessionCommand(
        String missionId,
        String taskId,
        String userId,
        String title,
        Map<String, Object> context) {
    public CreateAgentSessionCommand {
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
