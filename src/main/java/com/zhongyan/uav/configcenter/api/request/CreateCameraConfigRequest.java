package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateCameraConfigRequest(
        String cameraName,
        String cameraType,
        Map<String, Object> fov,
        Map<String, Object> parameters,
        String createdBy) {
}
