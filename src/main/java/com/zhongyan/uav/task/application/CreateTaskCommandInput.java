package com.zhongyan.uav.task.application;

import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.util.Map;

public record CreateTaskCommandInput(
        TaskCommandType commandType,
        Map<String, Object> payload,
        String idempotencyKey,
        String requestedBy,
        RiskLevel riskLevel,
        String deviceId,
        String reason) {
}
