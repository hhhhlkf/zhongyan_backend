package com.zhongyan.uav.task.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record TaskCommand(
        String commandId,
        String taskId,
        String missionId,
        String deviceId,
        TaskCommandType commandType,
        Map<String, Object> payload,
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
     * 校验命令必填字段，并规范化 payload 和风险等级。
     */
    public TaskCommand {
        requireText(commandId, "commandId");
        requireText(taskId, "taskId");
        requireText(missionId, "missionId");
        Objects.requireNonNull(commandType, "commandType must not be null");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        requireText(idempotencyKey, "idempotencyKey");
        requireText(requestedBy, "requestedBy");
        riskLevel = riskLevel == null ? commandType.defaultRiskLevel() : riskLevel;
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * 创建 TaskCommand，并根据风险等级和命令类型决定是否进入待审批状态。
     */
    public static TaskCommand create(String commandId, String taskId, String missionId, String deviceId,
                                     TaskCommandType commandType, Map<String, Object> payload,
                                     String idempotencyKey, String requestedBy, RiskLevel riskLevel,
                                     String reason, Instant now) {
        RiskLevel effectiveRiskLevel = riskLevel == null ? commandType.defaultRiskLevel() : riskLevel;
        boolean approvalRequired = effectiveRiskLevel.requiresApproval() || commandType.requiresApproval();
        TaskCommandStatus initialStatus = approvalRequired
                ? TaskCommandStatus.PENDING_APPROVAL
                : TaskCommandStatus.PENDING_DISPATCH;
        return new TaskCommand(commandId, taskId, missionId, deviceId, commandType, payload,
                idempotencyKey, requestedBy, effectiveRiskLevel, approvalRequired, null, null,
                initialStatus, now, null, null, reason);
    }

    /**
     * 审批通过待审批命令，使其具备下发条件。
     */
    public TaskCommand approve(String approver, Instant now) {
        if (!requiresApproval || status != TaskCommandStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Command is not waiting for approval");
        }
        requireText(approver, "approver");
        return copy(approver, now, TaskCommandStatus.PENDING_DISPATCH, dispatchedAt, completedAt);
    }

    /**
     * 拒绝待审批命令，并将命令置为终态。
     */
    public TaskCommand reject(String approver, Instant now) {
        if (!requiresApproval || status != TaskCommandStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Command is not waiting for approval");
        }
        requireText(approver, "approver");
        return copy(approver, now, TaskCommandStatus.REJECTED, dispatchedAt, now);
    }

    /**
     * 标记命令已下发，只允许无需审批或已审批通过的命令下发。
     */
    public TaskCommand markDispatched(Instant now) {
        if (status != TaskCommandStatus.PENDING_DISPATCH && status != TaskCommandStatus.APPROVED) {
            throw new IllegalStateException("Command cannot be dispatched from " + status);
        }
        return copy(approvedBy, approvedAt, TaskCommandStatus.DISPATCHED, now, completedAt);
    }

    /**
     * 标记命令执行完成，只允许已下发命令完成。
     */
    public TaskCommand complete(Instant now) {
        if (status != TaskCommandStatus.DISPATCHED) {
            throw new IllegalStateException("Command can only complete after dispatch");
        }
        return copy(approvedBy, approvedAt, TaskCommandStatus.COMPLETED, dispatchedAt, now);
    }

    public TaskCommand fail(Instant now) {
        if (status != TaskCommandStatus.DISPATCHED) {
            throw new IllegalStateException("Command can only fail after dispatch");
        }
        return copy(approvedBy, approvedAt, TaskCommandStatus.FAILED, dispatchedAt, now);
    }

    /**
     * 复制命令聚合并替换审批、下发和完成相关字段。
     */
    private TaskCommand copy(String nextApprovedBy, Instant nextApprovedAt, TaskCommandStatus nextStatus,
                             Instant nextDispatchedAt, Instant nextCompletedAt) {
        return new TaskCommand(commandId, taskId, missionId, deviceId, commandType, payload,
                idempotencyKey, requestedBy, riskLevel, requiresApproval, nextApprovedBy, nextApprovedAt,
                nextStatus, createdAt, nextDispatchedAt, nextCompletedAt, reason);
    }

    /**
     * 校验文本字段必须有实际内容。
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
