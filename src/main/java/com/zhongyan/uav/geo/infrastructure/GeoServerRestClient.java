package com.zhongyan.uav.geo.infrastructure;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;
import com.zhongyan.uav.geo.domain.LayerStatus;
import com.zhongyan.uav.geo.port.GeoServerPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "bms.geo.geoserver", name = "enabled", havingValue = "true")
public class GeoServerRestClient implements GeoServerPort {
    @Override
    public LayerPublishRecord publishLayer(Asset asset, Map<String, Object> parameters) {
        Object layerUrl = parameters.get("layerUrl");
        if (layerUrl == null || layerUrl.toString().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "layerUrl must be provided until GeoServer REST publishing is configured");
        }
        String layerId = parameters.getOrDefault("layerId", asset.assetId() + "-layer").toString();
        return new LayerPublishRecord(asset.assetId(), layerId, layerUrl.toString(),
                LayerStatus.PUBLISHED, Map.of("adapter", "geoserver-rest"), Instant.now());
    }
}
