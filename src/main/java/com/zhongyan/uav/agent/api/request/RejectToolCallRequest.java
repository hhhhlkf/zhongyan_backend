package com.zhongyan.uav.agent.api.request;

import jakarta.validation.constraints.NotBlank;

public record RejectToolCallRequest(
        @NotBlank(message = "reviewer must not be blank")
        String reviewer,
        @NotBlank(message = "reason must not be blank")
        String reason) {
}
