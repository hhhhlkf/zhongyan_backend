package com.zhongyan.uav.geo.domain;

import java.time.Instant;
import java.util.Map;

public record LayerPublishRecord(
        String assetId,
        String layerId,
        String layerUrl,
        LayerStatus status,
        Map<String, Object> metadata,
        Instant publishedAt) {
    public LayerPublishRecord {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (status == null) {
            status = LayerStatus.PENDING_APPROVAL;
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
