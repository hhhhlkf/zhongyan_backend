package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
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
@IdClass(JpaModelConfigId.class)
@Table(name = "model_config")
public class JpaModelConfigEntity {
    @Id
    @Column(name = "model_config_id", length = 120)
    private String modelConfigId;

    @Id
    @Column(name = "version")
    private int version;

    @Column(name = "model_type", nullable = false, length = 100)
    private String modelType;

    @Column(name = "runtime_type", nullable = false, length = 80)
    private String runtimeType;

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

    protected JpaModelConfigEntity() {
    }

    public static JpaModelConfigEntity fromDomain(ModelConfigVersion config) {
        JpaModelConfigEntity entity = new JpaModelConfigEntity();
        entity.modelConfigId = config.modelConfigId();
        entity.version = config.version();
        entity.modelType = config.modelType();
        entity.runtimeType = config.runtimeType();
        entity.parameters = config.parameters();
        entity.status = config.status();
        entity.createdBy = config.createdBy();
        entity.createdAt = config.createdAt();
        entity.updatedAt = config.updatedAt();
        return entity;
    }

    public ModelConfigVersion toDomain() {
        return new ModelConfigVersion(modelConfigId, version, modelType, runtimeType, parameters,
                status, createdBy, createdAt, updatedAt);
    }
}
