package com.zhongyan.uav.configcenter.api.response;

import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;

import java.time.Instant;
import java.util.Map;

public record CameraConfigVersionView(
        String cameraConfigId,
        int version,
        String cameraType,
        String status,
        Map<String, Object> fov,
        Instant updatedAt) {
    /**
     * 构建相机配置版本占位视图，避免 API 空壳接入真实存储。
     */
    public static CameraConfigVersionView placeholder(String cameraConfigId, int version, String cameraType) {
        return new CameraConfigVersionView(cameraConfigId, version, cameraType,
                "PLACEHOLDER", Map.of(), Instant.now());
    }

    public static CameraConfigVersionView from(CameraConfigVersion config) {
        return new CameraConfigVersionView(config.cameraConfigId(), config.version(), config.cameraType(),
                config.status().name(), config.fov(), config.updatedAt());
    }
}
