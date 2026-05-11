package com.zhongyan.uav.task.api.request;

import com.zhongyan.uav.task.application.CreateTaskCommandInput;
import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.util.Map;
import java.util.UUID;

public record CreateTaskCommandRequest(
        TaskCommandType commandType,
        Map<String, Object> payload,
        String idempotencyKey,
        String requestedBy,
        RiskLevel riskLevel,
        String deviceId,
        String reason) {
    /**
     * 将 API 请求转换为应用服务输入对象。
     */
    public CreateTaskCommandInput toInput() {
        return new CreateTaskCommandInput(commandType, payload,
                idempotencyKey == null || idempotencyKey.isBlank() ? "api-" + UUID.randomUUID() : idempotencyKey,
                requestedBy == null || requestedBy.isBlank() ? "mock-user" : requestedBy,
                riskLevel, deviceId, reason);
    }
}
