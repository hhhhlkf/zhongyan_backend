package com.zhongyan.uav.asset.port;

import java.util.Map;

public record PreviewRequest(
        String assetId,
        String sourceObjectKey,
        String previewObjectKey,
        Map<String, Object> parameters) {
    public PreviewRequest {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        if (sourceObjectKey == null || sourceObjectKey.isBlank()) {
            throw new IllegalArgumentException("sourceObjectKey must not be blank");
        }
        if (previewObjectKey == null || previewObjectKey.isBlank()) {
            throw new IllegalArgumentException("previewObjectKey must not be blank");
        }
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
