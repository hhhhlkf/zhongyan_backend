package com.zhongyan.uav.configcenter.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record DeviceConfig(
        String deviceId,
        String deviceName,
        String deviceType,
        String host,
        Map<String, Object> connection,
        Map<String, Object> capabilities,
        ConfigStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
    public DeviceConfig {
        requireText(deviceId, "deviceId");
        requireText(deviceName, "deviceName");
        requireText(deviceType, "deviceType");
        connection = connection == null ? Map.of() : Map.copyOf(connection);
        capabilities = capabilities == null ? Map.of() : Map.copyOf(capabilities);
        status = status == null ? ConfigStatus.DRAFT : status;
        createdBy = defaultText(createdBy, "mock-user");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static DeviceConfig create(String deviceId, String deviceName, String deviceType, String host,
                                      Map<String, Object> connection, Map<String, Object> capabilities,
                                      String createdBy, Instant now) {
        return new DeviceConfig(deviceId, deviceName, deviceType, host, connection, capabilities,
                ConfigStatus.DRAFT, createdBy, now, now);
    }

    public DeviceConfig disable(Instant now) {
        return copy(ConfigStatus.DISABLED, now);
    }

    private DeviceConfig copy(ConfigStatus nextStatus, Instant now) {
        return new DeviceConfig(deviceId, deviceName, deviceType, host, connection, capabilities,
                nextStatus, createdBy, createdAt, now);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
