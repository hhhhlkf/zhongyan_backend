package com.zhongyan.uav.geo.infrastructure.mock;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;
import com.zhongyan.uav.geo.domain.LayerStatus;
import com.zhongyan.uav.geo.port.GeoServerPort;

import java.time.Instant;
import java.util.Map;

public class MockGeoServerPort implements GeoServerPort {
    @Override
    public LayerPublishRecord publishLayer(Asset asset, Map<String, Object> parameters) {
        String layerId = parameters.getOrDefault("layerId", asset.assetId() + "-layer").toString();
        String layerUrl = parameters.getOrDefault("layerUrl", "/v2/layers/mock/" + layerId).toString();
        return new LayerPublishRecord(asset.assetId(), layerId, layerUrl, LayerStatus.PUBLISHED,
                Map.of("mock", true), Instant.now());
    }
}
