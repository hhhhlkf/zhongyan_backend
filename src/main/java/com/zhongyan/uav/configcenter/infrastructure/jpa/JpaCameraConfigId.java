package com.zhongyan.uav.configcenter.infrastructure.jpa;

import java.io.Serializable;
import java.util.Objects;

public class JpaCameraConfigId implements Serializable {
    private String cameraConfigId;
    private int version;

    public JpaCameraConfigId() {
    }

    public JpaCameraConfigId(String cameraConfigId, int version) {
        this.cameraConfigId = cameraConfigId;
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof JpaCameraConfigId other)) {
            return false;
        }
        return version == other.version && Objects.equals(cameraConfigId, other.cameraConfigId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cameraConfigId, version);
    }
}
