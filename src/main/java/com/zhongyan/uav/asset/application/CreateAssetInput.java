package com.zhongyan.uav.asset.application;

import java.util.Map;

public record CreateAssetInput(
        String missionId,
        String taskId,
        String assetType,
        String role,
        String name,
        String objectKey,
        String contentType,
        long sizeBytes,
        String checksum,
        Map<String, Object> metadata,
        String createdBy) {
}
