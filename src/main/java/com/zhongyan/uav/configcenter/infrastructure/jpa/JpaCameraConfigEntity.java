package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@IdClass(JpaCameraConfigId.class)
@Table(name = "camera_config")
public class JpaCameraConfigEntity {
    @Id
    @Column(name = "camera_config_id", length = 120)
    private String cameraConfigId;

    @Id
    @Column(name = "version")
    private int version;

    @Column(name = "camera_type", nullable = false, length = 80)
    private String cameraType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fov", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> fov = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> parameters = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConfigStatus status;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaCameraConfigEntity() {
    }

    public static JpaCameraConfigEntity fromDomain(CameraConfigVersion config) {
        JpaCameraConfigEntity entity = new JpaCameraConfigEntity();
        entity.cameraConfigId = config.cameraConfigId();
        entity.version = config.version();
        entity.cameraType = config.cameraType();
        entity.fov = config.fov();
        entity.parameters = config.parameters();
        entity.status = config.status();
        entity.createdBy = config.createdBy();
        entity.createdAt = config.createdAt();
        entity.updatedAt = config.updatedAt();
        return entity;
    }

    public CameraConfigVersion toDomain() {
        return new CameraConfigVersion(cameraConfigId, version, cameraType, fov, parameters,
                status, createdBy, createdAt, updatedAt);
    }
}
