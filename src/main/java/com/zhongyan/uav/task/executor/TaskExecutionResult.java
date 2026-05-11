package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.device.domain.DeviceCommandResult;

import java.time.Instant;
import java.util.Map;

public record TaskExecutionResult(
        boolean success,
        String errorCode,
        String errorMessage,
        String rawLogObjectKey,
        Map<String, Object> metadata,
        Instant completedAt) {
    public TaskExecutionResult {
        errorCode = errorCode == null ? (success ? "0" : "TASK_EXECUTION_FAILED") : errorCode;
        errorMessage = errorMessage == null ? "" : errorMessage;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        completedAt = completedAt == null ? Instant.now() : completedAt;
    }

    public static TaskExecutionResult fromDeviceResult(DeviceCommandResult result) {
        return new TaskExecutionResult(result.success(), result.exitCode(), result.message(),
                result.rawLogObjectKey(), result.metadata(), result.completedAt());
    }
}
