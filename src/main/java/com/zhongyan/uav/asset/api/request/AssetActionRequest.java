package com.zhongyan.uav.asset.api.request;

import java.util.Map;

public record AssetActionRequest(
        String requestedBy,
        String reason,
        Map<String, Object> parameters) {
}
