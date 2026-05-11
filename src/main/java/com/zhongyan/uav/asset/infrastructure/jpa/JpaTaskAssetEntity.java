package com.zhongyan.uav.asset.infrastructure.jpa;

import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.TaskAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@IdClass(JpaTaskAssetId.class)
@Table(name = "task_asset")
public class JpaTaskAssetEntity {
    @Id
    @Column(name = "task_id", length = 80)
    private String taskId;

    @Id
    @Column(name = "asset_id", length = 80)
    private String assetId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 40)
    private AssetRole role;

    @Column(name = "bound_at", nullable = false)
    private Instant boundAt;

    protected JpaTaskAssetEntity() {
    }

    public static JpaTaskAssetEntity fromDomain(TaskAsset taskAsset) {
        JpaTaskAssetEntity entity = new JpaTaskAssetEntity();
        entity.taskId = taskAsset.taskId();
        entity.assetId = taskAsset.assetId();
        entity.role = taskAsset.role();
        entity.boundAt = taskAsset.boundAt();
        return entity;
    }

    public TaskAsset toDomain() {
        return new TaskAsset(taskId, assetId, role, boundAt);
    }
}
