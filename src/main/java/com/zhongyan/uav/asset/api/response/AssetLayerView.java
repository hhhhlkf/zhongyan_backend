package com.zhongyan.uav.asset.api.response;

import java.time.Instant;

public record AssetLayerView(
        String assetId,
        String layerId,
        String layerUrl,
        String status,
        Instant updatedAt) {
    /**
     * 构建资产图层占位视图，避免 API 空壳调用 GeoServer。
     */
    public static AssetLayerView placeholder(String assetId, String layerId) {
        return new AssetLayerView(assetId, layerId, null, "PLACEHOLDER", Instant.now());
    }
}
