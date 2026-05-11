package com.zhongyan.uav.device.domain;

import java.time.Instant;
import java.util.Map;

public record DeviceHealth(
        String targetId,
        boolean reachable,
        String status,
        String message,
        Map<String, Object> details,
        Instant checkedAt) {
    public DeviceHealth {
        requireText(targetId, "targetId");
        status = defaultText(status, reachable ? "UP" : "DOWN");
        details = details == null ? Map.of() : Map.copyOf(details);
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }

    public static DeviceHealth up(String targetId, String message, Map<String, Object> details, Instant checkedAt) {
        return new DeviceHealth(targetId, true, "UP", message, details, checkedAt);
    }

    public static DeviceHealth down(String targetId, String message, Map<String, Object> details, Instant checkedAt) {
        return new DeviceHealth(targetId, false, "DOWN", message, details, checkedAt);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
