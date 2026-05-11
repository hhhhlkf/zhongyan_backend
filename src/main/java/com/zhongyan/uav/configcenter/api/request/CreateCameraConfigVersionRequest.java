package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateCameraConfigVersionRequest(
        Integer version,
        String cameraType,
        Map<String, Object> fov,
        Map<String, Object> parameters,
        String createdBy) {
    /**
     * 返回请求版本号，未传时使用初始版本号。
     */
    public int versionOrDefault() {
        return version == null ? 1 : version;
    }
}
