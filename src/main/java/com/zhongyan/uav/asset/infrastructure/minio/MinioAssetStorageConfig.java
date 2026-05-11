package com.zhongyan.uav.asset.infrastructure.minio;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "bms.asset.storage", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MinioAssetStorageProperties.class)
public class MinioAssetStorageConfig {
    @Bean
    public MinioClient minioClient(MinioAssetStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .region(properties.region())
                .build();
    }
}
