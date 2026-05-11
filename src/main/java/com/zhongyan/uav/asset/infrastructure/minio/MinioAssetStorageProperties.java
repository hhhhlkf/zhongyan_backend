package com.zhongyan.uav.asset.infrastructure.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "bms.asset.storage")
public record MinioAssetStorageProperties(
        boolean enabled,
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region,
        Duration presignedExpiry) {
    public MinioAssetStorageProperties {
        if (enabled) {
            requireText(endpoint, "endpoint");
            requireText(accessKey, "accessKey");
            requireText(secretKey, "secretKey");
            requireText(bucket, "bucket");
        }
        region = region == null || region.isBlank() ? "us-east-1" : region;
        presignedExpiry = presignedExpiry == null ? Duration.ofMinutes(15) : presignedExpiry;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("bms.asset.storage." + name + " must not be blank");
        }
    }
}
