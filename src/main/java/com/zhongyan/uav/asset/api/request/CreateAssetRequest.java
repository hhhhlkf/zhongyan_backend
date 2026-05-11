package com.zhongyan.uav.asset.api.request;

import java.util.Map;

public record CreateAssetRequest(
        String missionId,
        String taskId,
        String assetType,
        String role,
        String name,
        String objectKey,
        String contentType,
        Long sizeBytes,
        String checksum,
        Map<String, Object> metadata,
        String createdBy) {
}
