package com.zhongyan.uav.asset.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record Asset(
        String assetId,
        String missionId,
        String taskId,
        AssetType assetType,
        AssetRole assetRole,
        AssetStatus status,
        GeoStatus geoStatus,
        String name,
        String objectKey,
        String contentType,
        long sizeBytes,
        String checksum,
        String previewObjectKey,
        String layerUrl,
        Map<String, Object> metadata,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * 校验 Asset 聚合字段，并为角色、状态、地理状态设置默认值。
     */
    public Asset {
        requireText(assetId, "assetId");
        requireText(missionId, "missionId");
        Objects.requireNonNull(assetType, "assetType must not be null");
        assetRole = assetRole == null ? AssetRole.OUTPUT : assetRole;
        status = status == null ? AssetStatus.CREATED : status;
        geoStatus = geoStatus == null ? GeoStatus.NOT_REQUIRED : geoStatus;
        requireText(name, "name");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        requireText(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    /**
     * 创建一个新资产元数据记录，后续由存储或处理流程标记为可用。
     */
    public static Asset created(String assetId, String missionId, String taskId, AssetType assetType,
                                AssetRole assetRole, String name, String objectKey, String contentType,
                                long sizeBytes, String checksum, Map<String, Object> metadata,
                                String createdBy, Instant now) {
        return new Asset(assetId, missionId, taskId, assetType, assetRole, AssetStatus.CREATED,
                defaultGeoStatus(assetType), name, objectKey, contentType, sizeBytes, checksum,
                null, null, metadata, createdBy, now, now);
    }

    /**
     * 标记资产已经可被业务读取或引用。
     */
    public Asset markAvailable(Instant now) {
        return copy(AssetStatus.AVAILABLE, geoStatus, previewObjectKey, layerUrl, now);
    }

    /**
     * 标记资产处理失败。
     */
    public Asset markFailed(Instant now) {
        return copy(AssetStatus.FAILED, geoStatus, previewObjectKey, layerUrl, now);
    }

    /**
     * 标记资产空间信息已经计算完成。
     */
    public Asset markGeoCalculated(Instant now) {
        return copy(status, GeoStatus.CALCULATED, previewObjectKey, layerUrl, now);
    }

    /**
     * 标记资产空间信息计算失败，但不直接影响资产自身可用状态。
     */
    public Asset markGeoFailed(Instant now) {
        return copy(status, GeoStatus.FAILED, previewObjectKey, layerUrl, now);
    }

    /**
     * 绑定资产预览对象 key。
     */
    public Asset attachPreview(String nextPreviewObjectKey, Instant now) {
        requireText(nextPreviewObjectKey, "previewObjectKey");
        return copy(status, geoStatus, nextPreviewObjectKey, layerUrl, now);
    }

    /**
     * 绑定图层发布地址，发布动作本身需要在应用层走审批。
     */
    public Asset publishLayer(String nextLayerUrl, Instant now) {
        requireText(nextLayerUrl, "layerUrl");
        return copy(status, geoStatus, previewObjectKey, nextLayerUrl, now);
    }

    public Asset withMetadata(Map<String, Object> nextMetadata, Instant now) {
        return new Asset(assetId, missionId, taskId, assetType, assetRole, status, geoStatus,
                name, objectKey, contentType, sizeBytes, checksum, previewObjectKey, layerUrl,
                nextMetadata, createdBy, createdAt, now);
    }

    /**
     * 将资产标记为删除，实际对象删除由存储端口适配器处理。
     */
    public Asset delete(Instant now) {
        return copy(AssetStatus.DELETED, geoStatus, previewObjectKey, layerUrl, now);
    }

    /**
     * 复制 Asset 聚合并替换发生变化的字段，保持领域对象不可变。
     */
    private Asset copy(AssetStatus nextStatus, GeoStatus nextGeoStatus, String nextPreviewObjectKey,
                       String nextLayerUrl, Instant nextUpdatedAt) {
        return new Asset(assetId, missionId, taskId, assetType, assetRole, nextStatus, nextGeoStatus,
                name, objectKey, contentType, sizeBytes, checksum, nextPreviewObjectKey, nextLayerUrl,
                metadata, createdBy, createdAt, nextUpdatedAt);
    }

    /**
     * 根据资产类型判断是否默认需要空间计算。
     */
    private static GeoStatus defaultGeoStatus(AssetType assetType) {
        return assetType == AssetType.IMAGE || assetType == AssetType.VIDEO || assetType == AssetType.GEOMETRY
                ? GeoStatus.PENDING
                : GeoStatus.NOT_REQUIRED;
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
