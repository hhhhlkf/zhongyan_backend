package com.zhongyan.uav.task.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Task(
        String taskId,
        String missionId,
        TaskType taskType,
        TaskStatus status,
        int priority,
        String deviceId,
        String modelId,
        Map<String, Object> configSnapshot,
        List<String> inputAssetIds,
        List<String> outputAssetIds,
        BigDecimal progress,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant endedAt,
        String errorCode,
        String errorMessage) {
    /**
     * 校验 Task 聚合字段，并把空集合字段规范化为不可变空集合。
     */
    public Task {
        requireText(taskId, "taskId");
        requireText(missionId, "missionId");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        configSnapshot = configSnapshot == null ? Map.of() : Map.copyOf(configSnapshot);
        inputAssetIds = inputAssetIds == null ? List.of() : List.copyOf(inputAssetIds);
        outputAssetIds = outputAssetIds == null ? List.of() : List.copyOf(outputAssetIds);
        progress = progress == null ? BigDecimal.ZERO : progress;
        if (progress.compareTo(BigDecimal.ZERO) < 0 || progress.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("progress must be between 0 and 100");
        }
        requireText(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    /**
     * 创建草稿 Task，后续由应用服务提交进入审批或排队。
     */
    public static Task draft(String taskId, String missionId, TaskType taskType, int priority,
                             String deviceId, String modelId, Map<String, Object> configSnapshot,
                             List<String> inputAssetIds, String createdBy, Instant now) {
        return new Task(taskId, missionId, taskType, TaskStatus.DRAFT, priority, deviceId, modelId,
                configSnapshot, inputAssetIds, List.of(), BigDecimal.ZERO, createdBy, now, now,
                null, null, null, null);
    }

    /**
     * 按状态机规则切换 Task 状态，并同步开始时间、结束时间和完成进度。
     */
    public Task transitionTo(TaskStatus nextStatus, Instant now) {
        TaskStateMachine.assertCanTransition(status, nextStatus);
        Instant nextStartedAt = startedAt;
        Instant nextEndedAt = endedAt;
        BigDecimal nextProgress = progress;

        if (nextStatus == TaskStatus.RUNNING && nextStartedAt == null) {
            nextStartedAt = now;
        }
        if (nextStatus == TaskStatus.COMPLETED) {
            nextProgress = BigDecimal.valueOf(100);
            nextEndedAt = now;
        } else if (nextStatus.isTerminal()) {
            nextEndedAt = now;
        }

        return copy(nextStatus, outputAssetIds, nextProgress, now, nextStartedAt, nextEndedAt, errorCode, errorMessage);
    }

    /**
     * 更新运行中 Task 的执行进度。
     */
    public Task updateProgress(BigDecimal nextProgress, Instant now) {
        if (status != TaskStatus.RUNNING && status != TaskStatus.WAITING_ASSET && status != TaskStatus.POST_PROCESSING) {
            throw new IllegalStateException("Task progress can only update while task is active");
        }
        return copy(status, outputAssetIds, nextProgress, now, startedAt, endedAt, errorCode, errorMessage);
    }

    /**
     * 绑定 Task 产出的 Asset，重复绑定同一个 Asset 时保持幂等。
     */
    public Task bindOutputAsset(String assetId, Instant now) {
        requireText(assetId, "assetId");
        List<String> nextOutputAssetIds = new java.util.ArrayList<>(outputAssetIds);
        if (!nextOutputAssetIds.contains(assetId)) {
            nextOutputAssetIds.add(assetId);
        }
        return copy(status, nextOutputAssetIds, progress, now, startedAt, endedAt, errorCode, errorMessage);
    }

    /**
     * 将非终态 Task 标记为失败，并记录错误码和错误消息。
     */
    public Task fail(String nextErrorCode, String nextErrorMessage, Instant now) {
        TaskStateMachine.assertCanTransition(status, TaskStatus.FAILED);
        return copy(TaskStatus.FAILED, outputAssetIds, progress, now, startedAt, now, nextErrorCode, nextErrorMessage);
    }

    /**
     * 复制 Task 聚合并替换发生变化的字段，保持领域对象不可变。
     */
    private Task copy(TaskStatus nextStatus, List<String> nextOutputAssetIds, BigDecimal nextProgress,
                      Instant nextUpdatedAt, Instant nextStartedAt, Instant nextEndedAt,
                      String nextErrorCode, String nextErrorMessage) {
        return new Task(taskId, missionId, taskType, nextStatus, priority, deviceId, modelId,
                configSnapshot, inputAssetIds, nextOutputAssetIds, nextProgress, createdBy,
                createdAt, nextUpdatedAt, nextStartedAt, nextEndedAt, nextErrorCode, nextErrorMessage);
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
