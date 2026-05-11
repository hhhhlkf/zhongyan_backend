package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "task")
public class JpaTaskEntity {
    @Id
    @Column(name = "task_id", length = 80)
    private String taskId;

    @Column(name = "mission_id", nullable = false, length = 80)
    private String missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 64)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "model_id", length = 120)
    private String modelId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configSnapshot = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_asset_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> inputAssetIds = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_asset_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> outputAssetIds = List.of();

    @Column(name = "progress", nullable = false, precision = 5, scale = 2)
    private BigDecimal progress;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    protected JpaTaskEntity() {
    }

    public static JpaTaskEntity fromDomain(Task task) {
        JpaTaskEntity entity = new JpaTaskEntity();
        entity.taskId = task.taskId();
        entity.missionId = task.missionId();
        entity.taskType = task.taskType();
        entity.status = task.status();
        entity.priority = task.priority();
        entity.deviceId = task.deviceId();
        entity.modelId = task.modelId();
        entity.configSnapshot = task.configSnapshot();
        entity.inputAssetIds = task.inputAssetIds();
        entity.outputAssetIds = task.outputAssetIds();
        entity.progress = task.progress();
        entity.createdBy = task.createdBy();
        entity.createdAt = task.createdAt();
        entity.updatedAt = task.updatedAt();
        entity.startedAt = task.startedAt();
        entity.endedAt = task.endedAt();
        entity.errorCode = task.errorCode();
        entity.errorMessage = task.errorMessage();
        return entity;
    }

    public Task toDomain() {
        return new Task(taskId, missionId, taskType, status, priority, deviceId, modelId,
                configSnapshot, inputAssetIds, outputAssetIds, progress, createdBy, createdAt,
                updatedAt, startedAt, endedAt, errorCode, errorMessage);
    }
}
