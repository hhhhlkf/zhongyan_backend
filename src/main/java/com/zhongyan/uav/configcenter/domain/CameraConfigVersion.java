package com.zhongyan.uav.configcenter.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record CameraConfigVersion(
        String cameraConfigId,
        int version,
        String cameraType,
        Map<String, Object> fov,
        Map<String, Object> parameters,
        ConfigStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
    public CameraConfigVersion {
        requireText(cameraConfigId, "cameraConfigId");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than 0");
        }
        requireText(cameraType, "cameraType");
        fov = fov == null ? Map.of() : Map.copyOf(fov);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        status = status == null ? ConfigStatus.DRAFT : status;
        createdBy = defaultText(createdBy, "mock-user");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static CameraConfigVersion create(String cameraConfigId, int version, String cameraType,
                                             Map<String, Object> fov, Map<String, Object> parameters,
                                             String createdBy, Instant now) {
        return new CameraConfigVersion(cameraConfigId, version, cameraType, fov, parameters,
                ConfigStatus.DRAFT, createdBy, now, now);
    }

    public CameraConfigVersion activate(Instant now) {
        return copy(ConfigStatus.ACTIVE, now);
    }

    public CameraConfigVersion draft(Instant now) {
        return copy(ConfigStatus.DRAFT, now);
    }

    public CameraConfigVersion disable(Instant now) {
        return copy(ConfigStatus.DISABLED, now);
    }

    private CameraConfigVersion copy(ConfigStatus nextStatus, Instant now) {
        return new CameraConfigVersion(cameraConfigId, version, cameraType, fov, parameters,
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
