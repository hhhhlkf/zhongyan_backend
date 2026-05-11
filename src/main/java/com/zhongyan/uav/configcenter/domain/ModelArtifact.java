package com.zhongyan.uav.configcenter.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ModelArtifact(
        String artifactId,
        String modelConfigId,
        String artifactType,
        String objectKey,
        String checksum,
        Map<String, Object> metadata,
        ConfigStatus status,
        String createdBy,
        Instant createdAt) {
    public ModelArtifact {
        requireText(artifactId, "artifactId");
        requireText(modelConfigId, "modelConfigId");
        requireText(artifactType, "artifactType");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        status = status == null ? ConfigStatus.DRAFT : status;
        createdBy = defaultText(createdBy, "mock-user");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ModelArtifact create(String artifactId, String modelConfigId, String artifactType,
                                       String objectKey, String checksum, Map<String, Object> metadata,
                                       String createdBy, Instant now) {
        return new ModelArtifact(artifactId, modelConfigId, artifactType, objectKey, checksum, metadata,
                ConfigStatus.DRAFT, createdBy, now);
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
