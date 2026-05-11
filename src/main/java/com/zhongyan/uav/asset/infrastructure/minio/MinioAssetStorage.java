package com.zhongyan.uav.asset.infrastructure.minio;

import com.zhongyan.uav.asset.port.AssetObject;
import com.zhongyan.uav.asset.port.AssetStoragePort;
import com.zhongyan.uav.asset.port.StoredObject;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "bms.asset.storage", name = "enabled", havingValue = "true")
public class MinioAssetStorage implements AssetStoragePort {
    private final MinioClient minioClient;
    private final MinioAssetStorageProperties properties;

    public MinioAssetStorage(MinioClient minioClient, MinioAssetStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public StoredObject put(AssetObject object) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(object.objectKey())
                    .stream(object.content(), object.contentLength(), -1)
                    .contentType(object.contentType())
                    .userMetadata(object.metadata())
                    .build());
            return stat(object.objectKey())
                    .orElseThrow(() -> storageFailure("stored object is not readable after upload", null));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw storageFailure("failed to upload asset object: " + object.objectKey(), exception);
        }
    }

    @Override
    public Optional<StoredObject> get(String objectKey) {
        try {
            return stat(objectKey);
        } catch (Exception exception) {
            throw storageFailure("failed to inspect asset object: " + objectKey, exception);
        }
    }

    @Override
    public InputStream read(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("failed to read asset object: " + objectKey, exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("failed to delete asset object: " + objectKey, exception);
        }
    }

    @Override
    public String createReadUrl(String objectKey, Duration expiry) {
        Duration effectiveExpiry = expiry == null ? properties.presignedExpiry() : expiry;
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(Math.toIntExact(effectiveExpiry.toSeconds()), TimeUnit.SECONDS)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("failed to create asset object read url: " + objectKey, exception);
        }
    }

    private Optional<StoredObject> stat(String objectKey) throws Exception {
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
            return Optional.of(toStoredObject(response));
        } catch (ErrorResponseException exception) {
            String code = exception.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NoSuchBucket".equals(code)) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }

    private StoredObject toStoredObject(StatObjectResponse response) {
        Instant lastModified = response.lastModified() == null ? null : response.lastModified().toInstant();
        Map<String, String> metadata = response.userMetadata() == null ? Map.of() : response.userMetadata();
        return new StoredObject(
                response.bucket(),
                response.object(),
                response.size(),
                response.contentType(),
                response.etag(),
                lastModified,
                metadata);
    }

    private BusinessException storageFailure(String message, Throwable cause) {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
