package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateTransferConfigVersionRequest(
        Integer version,
        String transferType,
        Map<String, Object> endpoint,
        Map<String, Object> parameters,
        String createdBy) {
    /**
     * 返回请求版本号，未传时使用初始版本号。
     */
    public int versionOrDefault() {
        return version == null ? 1 : version;
    }
}
