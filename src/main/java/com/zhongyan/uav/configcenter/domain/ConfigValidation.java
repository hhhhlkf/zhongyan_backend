package com.zhongyan.uav.configcenter.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ConfigValidation(
        String validationId,
        String configType,
        String configId,
        ConfigValidationStatus status,
        List<String> errors,
        Instant createdAt) {
    public ConfigValidation {
        requireText(validationId, "validationId");
        requireText(configType, "configType");
        requireText(configId, "configId");
        status = status == null ? ConfigValidationStatus.PASSED : status;
        errors = errors == null ? List.of() : List.copyOf(errors);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ConfigValidation passed(String validationId, String configType, String configId, Instant now) {
        return new ConfigValidation(validationId, configType, configId,
                ConfigValidationStatus.PASSED, List.of(), now);
    }

    public static ConfigValidation failed(String validationId, String configType, String configId,
                                          List<String> errors, Instant now) {
        return new ConfigValidation(validationId, configType, configId,
                ConfigValidationStatus.FAILED, errors, now);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
