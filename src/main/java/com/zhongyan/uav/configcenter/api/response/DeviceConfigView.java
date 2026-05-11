package com.zhongyan.uav.configcenter.api.response;

import com.zhongyan.uav.configcenter.domain.DeviceConfig;

import java.time.Instant;

public record DeviceConfigView(
        String deviceId,
        String deviceName,
        String deviceType,
        String host,
        String status,
        Instant updatedAt) {
    /**
     * 构建设备配置占位视图，避免 API 空壳接入真实存储。
     */
    public static DeviceConfigView placeholder(String deviceId, String deviceName, String deviceType, String host) {
        return new DeviceConfigView(deviceId, deviceName, deviceType, host, "PLACEHOLDER", Instant.now());
    }

    public static DeviceConfigView from(DeviceConfig config) {
        return new DeviceConfigView(config.deviceId(), config.deviceName(), config.deviceType(),
                config.host(), config.status().name(), config.updatedAt());
    }
}
