package com.zhongyan.uav.task.api.response;

import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TaskView(
        String taskId,
        String missionId,
        TaskType taskType,
        TaskStatus status,
        int priority,
        String deviceId,
        String modelId,
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
     * 将 Task 领域对象转换为 API 响应视图。
     */
    public static TaskView from(Task task) {
        return new TaskView(task.taskId(), task.missionId(), task.taskType(), task.status(),
                task.priority(), task.deviceId(), task.modelId(), task.inputAssetIds(),
                task.outputAssetIds(), task.progress(), task.createdBy(), task.createdAt(),
                task.updatedAt(), task.startedAt(), task.endedAt(), task.errorCode(), task.errorMessage());
    }
}
