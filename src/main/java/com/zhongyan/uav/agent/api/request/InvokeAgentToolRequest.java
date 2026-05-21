package com.zhongyan.uav.agent.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.Set;

public record InvokeAgentToolRequest(
        @NotBlank(message = "sessionId must not be blank")
        String sessionId,
        @NotBlank(message = "messageId must not be blank")
        String messageId,
        @NotBlank(message = "userId must not be blank")
        String userId,
        @NotBlank(message = "toolName must not be blank")
        String toolName,
        Map<String, Object> input,
        Set<String> permissions) {
    public InvokeAgentToolRequest {
        input = input == null ? Map.of() : Map.copyOf(input);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
