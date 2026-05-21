package com.zhongyan.uav.agent.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record SendAgentMessageRequest(
        String role,
        @NotBlank(message = "content must not be blank")
        String content,
        Map<String, Object> attachments,
        @NotBlank(message = "createdBy must not be blank")
        String createdBy) {
    public SendAgentMessageRequest {
        attachments = attachments == null ? Map.of() : Map.copyOf(attachments);
    }
}
