package com.zhongyan.uav.configcenter.infrastructure.jpa;

import java.io.Serializable;
import java.util.Objects;

public class JpaTransferConfigId implements Serializable {
    private String transferConfigId;
    private int version;

    public JpaTransferConfigId() {
    }

    public JpaTransferConfigId(String transferConfigId, int version) {
        this.transferConfigId = transferConfigId;
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof JpaTransferConfigId other)) {
            return false;
        }
        return version == other.version && Objects.equals(transferConfigId, other.transferConfigId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferConfigId, version);
    }
}
