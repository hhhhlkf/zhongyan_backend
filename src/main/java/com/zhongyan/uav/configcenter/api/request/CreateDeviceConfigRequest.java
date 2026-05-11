package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateDeviceConfigRequest(
        String deviceId,
        String deviceName,
        String deviceType,
        String host,
        Map<String, Object> connection,
        Map<String, Object> capabilities,
        String createdBy) {
}
