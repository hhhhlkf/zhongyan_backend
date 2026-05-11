package com.zhongyan.uav.asset.domain;

import java.time.Instant;
import java.util.Objects;

public record TaskAsset(
        String taskId,
        String assetId,
        AssetRole role,
        Instant boundAt) {
    /**
     * 校验 Task 与 Asset 的绑定关系。
     */
    public TaskAsset {
        requireText(taskId, "taskId");
        requireText(assetId, "assetId");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(boundAt, "boundAt must not be null");
    }

    /**
     * 校验文本字段必须有实际内容。
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
