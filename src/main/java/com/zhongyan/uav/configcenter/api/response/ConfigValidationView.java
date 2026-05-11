package com.zhongyan.uav.configcenter.api.response;

import com.zhongyan.uav.configcenter.domain.ConfigValidation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConfigValidationView(
        String validationId,
        String configType,
        String configId,
        String status,
        List<String> errors,
        Instant createdAt) {
    /**
     * 构建新校验任务占位视图，后续由真实校验服务替换。
     */
    public static ConfigValidationView placeholder(String configType, String configId) {
        return placeholder("validation-" + UUID.randomUUID(), configType, configId);
    }

    /**
     * 构建指定校验编号的占位视图，用于详情查询空壳。
     */
    public static ConfigValidationView placeholder(String validationId, String configType, String configId) {
        return new ConfigValidationView(validationId, configType, configId, "PLACEHOLDER", List.of(), Instant.now());
    }

    public static ConfigValidationView from(ConfigValidation validation) {
        return new ConfigValidationView(validation.validationId(), validation.configType(),
                validation.configId(), validation.status().name(), validation.errors(), validation.createdAt());
    }
}
