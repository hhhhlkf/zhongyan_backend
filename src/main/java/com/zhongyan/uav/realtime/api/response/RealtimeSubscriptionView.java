package com.zhongyan.uav.realtime.api.response;

import java.time.Instant;

/**
 * 实时订阅视图。
 * <p>
 * 该 DTO 保留给非 SSE 的订阅管理接口使用，不承载实际事件流内容。
 */
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
