package com.zhongyan.uav.asset.port;

public record PreviewResult(
        String previewObjectKey,
        String contentType,
        long sizeBytes,
        String checksum) {
}
