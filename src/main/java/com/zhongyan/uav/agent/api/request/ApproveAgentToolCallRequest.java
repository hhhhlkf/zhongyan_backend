package com.zhongyan.uav.agent.api.request;

import jakarta.validation.constraints.NotBlank;

public record ApproveAgentToolCallRequest(
        @NotBlank(message = "reviewer must not be blank")
        String reviewer,
        String reason) {
}
