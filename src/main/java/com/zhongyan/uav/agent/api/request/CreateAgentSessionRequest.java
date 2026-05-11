package com.zhongyan.uav.agent.api.request;

import java.util.Map;

public record CreateAgentSessionRequest(
        String missionId,
        String taskId,
        String createdBy,
        Map<String, Object> context) {
}
