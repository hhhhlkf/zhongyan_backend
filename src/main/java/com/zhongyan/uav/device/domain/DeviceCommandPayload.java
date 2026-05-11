package com.zhongyan.uav.device.domain;

import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Duration;
import java.util.Map;

public record DeviceCommandPayload(
        String commandId,
        String taskId,
        String deviceId,
        TaskCommandType commandType,
        DeviceProtocol protocol,
        Map<String, Object> parameters,
        Duration timeout) {
    public DeviceCommandPayload {
        requireText(commandId, "commandId");
        requireText(taskId, "taskId");
        requireText(deviceId, "deviceId");
        if (commandType == null) {
            throw new IllegalArgumentException("commandType must not be null");
        }
        protocol = protocol == null ? DeviceProtocol.MOCK : protocol;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
