package com.zhongyan.uav.device.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record DeviceCommandResult(
        String commandId,
        String taskId,
        String deviceId,
        boolean success,
        String exitCode,
        String message,
        String rawOutput,
        String rawLogObjectKey,
        Duration elapsed,
        Map<String, Object> metadata,
        Instant completedAt) {
    public DeviceCommandResult {
        requireText(commandId, "commandId");
        requireText(taskId, "taskId");
        requireText(deviceId, "deviceId");
        exitCode = exitCode == null ? (success ? "0" : "1") : exitCode;
        rawOutput = rawOutput == null ? "" : rawOutput;
        rawLogObjectKey = rawLogObjectKey == null || rawLogObjectKey.isBlank()
                ? "device-logs/" + taskId + "/" + commandId + ".log"
                : rawLogObjectKey;
        elapsed = elapsed == null ? Duration.ZERO : elapsed;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        completedAt = completedAt == null ? Instant.now() : completedAt;
    }

    public static DeviceCommandResult success(DeviceCommandPayload payload, String message,
                                              String rawOutput, Map<String, Object> metadata,
                                              Instant completedAt) {
        return new DeviceCommandResult(payload.commandId(), payload.taskId(), payload.deviceId(),
                true, "0", message, rawOutput, null, Duration.ZERO, metadata, completedAt);
    }

    public static DeviceCommandResult failure(DeviceCommandPayload payload, String exitCode, String message,
                                              String rawOutput, Map<String, Object> metadata,
                                              Instant completedAt) {
        return new DeviceCommandResult(payload.commandId(), payload.taskId(), payload.deviceId(),
                false, exitCode, message, rawOutput, null, Duration.ZERO, metadata, completedAt);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
