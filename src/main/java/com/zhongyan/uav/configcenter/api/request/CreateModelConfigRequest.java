package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateModelConfigRequest(
        String modelName,
        String modelType,
        String runtimeType,
        Map<String, Object> parameters,
        String createdBy) {
}
