package com.zhongyan.uav.agent.api.response;

import com.zhongyan.uav.agent.domain.AgentToolCall;

import java.time.Instant;
import java.util.Map;

public record AgentToolCallView(
        String toolCallId,
        String sessionId,
        String toolName,
        String riskLevel,
        boolean approvalRequired,
        String requestedBy,
        String approvedBy,
        Instant approvedAt,
        String status,
        String errorMessage,
        Map<String, Object> input,
        Map<String, Object> output,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * 将领域审计记录转换为 API 视图，避免 Controller 直接暴露领域对象。
     */
    public static AgentToolCallView fromDomain(AgentToolCall toolCall) {
        return new AgentToolCallView(toolCall.toolCallId(), toolCall.sessionId(), toolCall.toolName(),
                toolCall.riskLevel().name(), toolCall.approvalRequired(), toolCall.requestedBy(),
                toolCall.approvedBy(), toolCall.approvedAt(), toolCall.status().name(),
                toolCall.errorMessage(), toolCall.input(), toolCall.output(), toolCall.createdAt(),
                toolCall.completedAt() == null ? toolCall.createdAt() : toolCall.completedAt());
    }
}
