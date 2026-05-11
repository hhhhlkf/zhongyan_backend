package com.zhongyan.uav.configcenter.infrastructure.jpa;

import java.io.Serializable;
import java.util.Objects;

public class JpaModelConfigId implements Serializable {
    private String modelConfigId;
    private int version;

    public JpaModelConfigId() {
    }

    public JpaModelConfigId(String modelConfigId, int version) {
        this.modelConfigId = modelConfigId;
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof JpaModelConfigId other)) {
            return false;
        }
        return version == other.version && Objects.equals(modelConfigId, other.modelConfigId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelConfigId, version);
    }
}
