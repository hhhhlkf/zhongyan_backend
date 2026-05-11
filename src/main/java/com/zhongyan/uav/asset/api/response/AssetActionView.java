package com.zhongyan.uav.asset.api.response;

import java.time.Instant;

public record AssetActionView(
        String assetId,
        String action,
        String status,
        Instant acceptedAt) {
    /**
     * 构建资产动作占位响应，表示请求已被 API 空壳接收。
     */
    public static AssetActionView accepted(String assetId, String action) {
        return new AssetActionView(assetId, action, "ACCEPTED", Instant.now());
    }
}
