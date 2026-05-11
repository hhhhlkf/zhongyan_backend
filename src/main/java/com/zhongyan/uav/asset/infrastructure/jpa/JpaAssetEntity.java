package com.zhongyan.uav.asset.infrastructure.jpa;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.GeoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "asset")
public class JpaAssetEntity {
    @Id
    @Column(name = "asset_id", length = 80)
    private String assetId;

    @Column(name = "mission_id", nullable = false, length = 80)
    private String missionId;

    @Column(name = "task_id", length = 80)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 40)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_role", nullable = false, length = 40)
    private AssetRole assetRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AssetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "geo_status", nullable = false, length = 32)
    private GeoStatus geoStatus;

    @Column(name = "name", nullable = false, length = 240)
    private String name;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum", length = 160)
    private String checksum;

    @Column(name = "preview_object_key", length = 500)
    private String previewObjectKey;

    @Column(name = "layer_url", length = 800)
    private String layerUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaAssetEntity() {
    }

    public static JpaAssetEntity fromDomain(Asset asset) {
        JpaAssetEntity entity = new JpaAssetEntity();
        entity.assetId = asset.assetId();
        entity.missionId = asset.missionId();
        entity.taskId = asset.taskId();
        entity.assetType = asset.assetType();
        entity.assetRole = asset.assetRole();
        entity.status = asset.status();
        entity.geoStatus = asset.geoStatus();
        entity.name = asset.name();
        entity.objectKey = asset.objectKey();
        entity.contentType = asset.contentType();
        entity.sizeBytes = asset.sizeBytes();
        entity.checksum = asset.checksum();
        entity.previewObjectKey = asset.previewObjectKey();
        entity.layerUrl = asset.layerUrl();
        entity.metadata = asset.metadata();
        entity.createdBy = asset.createdBy();
        entity.createdAt = asset.createdAt();
        entity.updatedAt = asset.updatedAt();
        return entity;
    }

    public Asset toDomain() {
        return new Asset(assetId, missionId, taskId, assetType, assetRole, status, geoStatus,
                name, objectKey, contentType, sizeBytes, checksum, previewObjectKey, layerUrl,
                metadata, createdBy, createdAt, updatedAt);
    }
}
