package com.zhongyan.uav.asset.port;

import java.time.Instant;
import java.util.Map;

public record StoredObject(
        String bucket,
        String objectKey,
        long contentLength,
        String contentType,
        String etag,
        Instant lastModified,
        Map<String, String> metadata) {
    public StoredObject {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
