package com.zhongyan.uav.asset.port;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public record AssetObject(
        String objectKey,
        InputStream content,
        long contentLength,
        String contentType,
        Map<String, String> metadata) {
    public AssetObject {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }
        Objects.requireNonNull(content, "content must not be null");
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
        contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
