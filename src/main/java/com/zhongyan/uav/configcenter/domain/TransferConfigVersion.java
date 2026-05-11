package com.zhongyan.uav.configcenter.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record TransferConfigVersion(
        String transferConfigId,
        int version,
        String transferType,
        Map<String, Object> endpoint,
        Map<String, Object> parameters,
        ConfigStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
    public TransferConfigVersion {
        requireText(transferConfigId, "transferConfigId");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than 0");
        }
        requireText(transferType, "transferType");
        endpoint = endpoint == null ? Map.of() : Map.copyOf(endpoint);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        status = status == null ? ConfigStatus.DRAFT : status;
        createdBy = defaultText(createdBy, "mock-user");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static TransferConfigVersion create(String transferConfigId, int version, String transferType,
                                               Map<String, Object> endpoint, Map<String, Object> parameters,
                                               String createdBy, Instant now) {
        return new TransferConfigVersion(transferConfigId, version, transferType, endpoint, parameters,
                ConfigStatus.DRAFT, createdBy, now, now);
    }

    public TransferConfigVersion activate(Instant now) {
        return copy(ConfigStatus.ACTIVE, now);
    }

    public TransferConfigVersion draft(Instant now) {
        return copy(ConfigStatus.DRAFT, now);
    }

    public TransferConfigVersion disable(Instant now) {
        return copy(ConfigStatus.DISABLED, now);
    }

    private TransferConfigVersion copy(ConfigStatus nextStatus, Instant now) {
        return new TransferConfigVersion(transferConfigId, version, transferType, endpoint, parameters,
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
