package com.zhongyan.uav.asset.api.response;

import java.time.Instant;

public record AssetView(
        String assetId,
        String assetType,
        String status,
        String name,
        String url,
        String previewUrl,
        String geoStatus,
        String layerUrl,
        Instant createdAt) {
    /**
     * 构建资产占位视图，避免 API 空壳接入真实对象存储。
     */
    public static AssetView placeholder(String assetId, String assetType, String name) {
        return new AssetView(assetId, assetType, "PLACEHOLDER", name,
                null, null, "UNKNOWN", null, Instant.now());
    }
}
