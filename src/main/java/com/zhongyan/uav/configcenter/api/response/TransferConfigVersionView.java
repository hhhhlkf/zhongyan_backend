package com.zhongyan.uav.configcenter.api.response;

import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;

import java.time.Instant;

public record TransferConfigVersionView(
        String transferConfigId,
        int version,
        String transferType,
        String status,
        Instant updatedAt) {
    /**
     * 构建传输配置版本占位视图，避免 API 空壳接入真实存储。
     */
    public static TransferConfigVersionView placeholder(String transferConfigId, int version, String transferType) {
        return new TransferConfigVersionView(transferConfigId, version, transferType, "PLACEHOLDER", Instant.now());
    }

    public static TransferConfigVersionView from(TransferConfigVersion config) {
        return new TransferConfigVersionView(config.transferConfigId(), config.version(),
                config.transferType(), config.status().name(), config.updatedAt());
    }
}
