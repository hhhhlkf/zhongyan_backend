package com.zhongyan.uav.configcenter.api.response;

import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;

import java.time.Instant;

public record ModelConfigVersionView(
        String modelConfigId,
        int version,
        String modelType,
        String runtimeType,
        String status,
        Instant updatedAt) {
    /**
     * 构建模型配置版本占位视图，避免 API 空壳接入真实存储。
     */
    public static ModelConfigVersionView placeholder(String modelConfigId, int version,
                                                     String modelType, String runtimeType) {
        return new ModelConfigVersionView(modelConfigId, version, modelType, runtimeType,
                "PLACEHOLDER", Instant.now());
    }

    public static ModelConfigVersionView from(ModelConfigVersion config) {
        return new ModelConfigVersionView(config.modelConfigId(), config.version(), config.modelType(),
                config.runtimeType(), config.status().name(), config.updatedAt());
    }
}
