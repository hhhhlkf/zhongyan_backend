package com.zhongyan.uav.configcenter.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ModelConfigVersion(
        String modelConfigId,
        int version,
        String modelType,
        String runtimeType,
        Map<String, Object> parameters,
        ConfigStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
    public ModelConfigVersion {
        requireText(modelConfigId, "modelConfigId");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than 0");
        }
        requireText(modelType, "modelType");
        requireText(runtimeType, "runtimeType");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        status = status == null ? ConfigStatus.DRAFT : status;
        createdBy = defaultText(createdBy, "mock-user");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static ModelConfigVersion create(String modelConfigId, int version, String modelType,
                                            String runtimeType, Map<String, Object> parameters,
                                            String createdBy, Instant now) {
        return new ModelConfigVersion(modelConfigId, version, modelType, runtimeType, parameters,
                ConfigStatus.DRAFT, createdBy, now, now);
    }

    public ModelConfigVersion activate(Instant now) {
        return copy(ConfigStatus.ACTIVE, now);
    }

    public ModelConfigVersion draft(Instant now) {
        return copy(ConfigStatus.DRAFT, now);
    }

    public ModelConfigVersion disable(Instant now) {
        return copy(ConfigStatus.DISABLED, now);
    }

    private ModelConfigVersion copy(ConfigStatus nextStatus, Instant now) {
        return new ModelConfigVersion(modelConfigId, version, modelType, runtimeType, parameters,
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
