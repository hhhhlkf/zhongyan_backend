package com.zhongyan.uav.agent.api.request;

import java.util.Map;

public record CreateAgentMessageRequest(
        String role,
        String content,
        Map<String, Object> attachments,
        String createdBy) {
}
