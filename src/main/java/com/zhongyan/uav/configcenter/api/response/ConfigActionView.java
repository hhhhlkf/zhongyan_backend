package com.zhongyan.uav.configcenter.api.response;

import java.time.Instant;

public record ConfigActionView(
        String configType,
        String configId,
        String action,
        String status,
        Instant acceptedAt) {
    /**
     * 构建配置动作占位响应，表示请求已被 API 空壳接收。
     */
    public static ConfigActionView accepted(String configType, String configId, String action) {
        return new ConfigActionView(configType, configId, action, "ACCEPTED", Instant.now());
    }
}
