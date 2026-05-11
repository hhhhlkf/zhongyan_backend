package com.zhongyan.uav.asset.infrastructure.jpa;

import com.zhongyan.uav.asset.domain.AssetRole;

import java.io.Serializable;
import java.util.Objects;

public class JpaTaskAssetId implements Serializable {
    private String taskId;
    private String assetId;
    private AssetRole role;

    public JpaTaskAssetId() {
    }

    public JpaTaskAssetId(String taskId, String assetId, AssetRole role) {
        this.taskId = taskId;
        this.assetId = assetId;
        this.role = role;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof JpaTaskAssetId other)) {
            return false;
        }
        return Objects.equals(taskId, other.taskId)
                && Objects.equals(assetId, other.assetId)
                && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, assetId, role);
    }
}
