package com.zhongyan.uav.agent.api.request;

public record ApproveAgentToolCallRequest(
        String reviewer,
        String reason) {
}
