package com.zhongyan.uav.configcenter.api.response;

import com.zhongyan.uav.configcenter.domain.ModelArtifact;

import java.time.Instant;

public record ModelArtifactView(
        String artifactId,
        String modelConfigId,
        String artifactType,
        String status,
        Instant createdAt) {
    /**
     * 构建模型制品占位视图，避免 API 空壳接入真实对象存储。
     */
    public static ModelArtifactView placeholder(String artifactId, String modelConfigId, String artifactType) {
        return new ModelArtifactView(artifactId, modelConfigId, artifactType, "PLACEHOLDER", Instant.now());
    }

    public static ModelArtifactView from(ModelArtifact artifact) {
        return new ModelArtifactView(artifact.artifactId(), artifact.modelConfigId(),
                artifact.artifactType(), artifact.status().name(), artifact.createdAt());
    }
}
