package com.zhongyan.uav.task.api.response;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.GeoStatus;

import java.time.Instant;

public record AssetView(
        String assetId,
        String missionId,
        String taskId,
        AssetType assetType,
        AssetRole assetRole,
        AssetStatus status,
        GeoStatus geoStatus,
        String name,
        String objectKey,
        String previewObjectKey,
        String layerUrl,
        Instant createdAt) {
    /**
     * 将 Asset 领域对象转换为 API 响应视图。
     */
    public static AssetView from(Asset asset) {
        return new AssetView(asset.assetId(), asset.missionId(), asset.taskId(), asset.assetType(),
                asset.assetRole(), asset.status(), asset.geoStatus(), asset.name(), asset.objectKey(),
                asset.previewObjectKey(), asset.layerUrl(), asset.createdAt());
    }
}
