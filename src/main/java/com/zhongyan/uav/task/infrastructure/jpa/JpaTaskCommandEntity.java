package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskCommandType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "task_command")
public class JpaTaskCommandEntity {
    @Id
    @Column(name = "command_id", length = 80)
    private String commandId;

    @Column(name = "task_id", nullable = false, length = 80)
    private String taskId;

    @Column(name = "mission_id", nullable = false, length = 80)
    private String missionId;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 80)
    private TaskCommandType commandType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TaskCommandStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "reason")
    private String reason;

    protected JpaTaskCommandEntity() {
    }

    public static JpaTaskCommandEntity fromDomain(TaskCommand command) {
        JpaTaskCommandEntity entity = new JpaTaskCommandEntity();
        entity.commandId = command.commandId();
        entity.taskId = command.taskId();
        entity.missionId = command.missionId();
        entity.deviceId = command.deviceId();
        entity.commandType = command.commandType();
        entity.payload = command.payload();
        entity.idempotencyKey = command.idempotencyKey();
        entity.requestedBy = command.requestedBy();
        entity.riskLevel = command.riskLevel();
        entity.requiresApproval = command.requiresApproval();
        entity.approvedBy = command.approvedBy();
        entity.approvedAt = command.approvedAt();
        entity.status = command.status();
        entity.createdAt = command.createdAt();
        entity.dispatchedAt = command.dispatchedAt();
        entity.completedAt = command.completedAt();
        entity.reason = command.reason();
        return entity;
    }

    public TaskCommand toDomain() {
        return new TaskCommand(commandId, taskId, missionId, deviceId, commandType, payload,
                idempotencyKey, requestedBy, riskLevel, requiresApproval, approvedBy, approvedAt,
                status, createdAt, dispatchedAt, completedAt, reason);
    }
}
