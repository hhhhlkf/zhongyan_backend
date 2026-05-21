package com.zhongyan.uav.agent.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateAgentSessionRequest(
        String missionId,
        String taskId,
        @NotBlank(message = "createdBy must not be blank")
        String createdBy,
        @NotBlank(message = "title must not be blank")
        String title,
        Map<String, Object> context) {
    public CreateAgentSessionRequest {
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
