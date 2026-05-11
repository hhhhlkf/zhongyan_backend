package com.zhongyan.uav.task.api.response;

import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Instant;

public record TaskCommandView(
        String commandId,
        String taskId,
        String missionId,
        String deviceId,
        TaskCommandType commandType,
        String idempotencyKey,
        String requestedBy,
        RiskLevel riskLevel,
        boolean requiresApproval,
        String approvedBy,
        Instant approvedAt,
        TaskCommandStatus status,
        Instant createdAt,
        Instant dispatchedAt,
        Instant completedAt,
        String reason) {
    /**
     * 将 TaskCommand 领域对象转换为 API 响应视图。
     */
    public static TaskCommandView from(TaskCommand command) {
        return new TaskCommandView(command.commandId(), command.taskId(), command.missionId(),
                command.deviceId(), command.commandType(), command.idempotencyKey(), command.requestedBy(),
                command.riskLevel(), command.requiresApproval(), command.approvedBy(), command.approvedAt(),
                command.status(), command.createdAt(), command.dispatchedAt(), command.completedAt(),
                command.reason());
    }
}
