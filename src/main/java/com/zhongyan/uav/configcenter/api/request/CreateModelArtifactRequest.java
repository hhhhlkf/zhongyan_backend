package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateModelArtifactRequest(
        String artifactType,
        String objectKey,
        String checksum,
        Map<String, Object> metadata,
        String createdBy) {
}
