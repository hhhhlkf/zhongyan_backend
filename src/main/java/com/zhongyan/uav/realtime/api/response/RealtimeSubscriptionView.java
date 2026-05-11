package com.zhongyan.uav.realtime.api.response;

import java.time.Instant;

public record RealtimeSubscriptionView(
        String scope,
        String targetId,
        String status,
        String transport,
        Instant createdAt) {
    /**
     * 构建实时订阅占位视图，避免 API 空壳启动真实推送连接。
     */
    public static RealtimeSubscriptionView placeholder(String scope, String targetId) {
        return new RealtimeSubscriptionView(scope, targetId, "PLACEHOLDER", "NONE", Instant.now());
    }
}
