package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
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
@IdClass(JpaTransferConfigId.class)
@Table(name = "transfer_config")
public class JpaTransferConfigEntity {
    @Id
    @Column(name = "transfer_config_id", length = 120)
    private String transferConfigId;

    @Id
    @Column(name = "version")
    private int version;

    @Column(name = "transfer_type", nullable = false, length = 80)
    private String transferType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endpoint", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> endpoint = Map.of();

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

    protected JpaTransferConfigEntity() {
    }

    public static JpaTransferConfigEntity fromDomain(TransferConfigVersion config) {
        JpaTransferConfigEntity entity = new JpaTransferConfigEntity();
        entity.transferConfigId = config.transferConfigId();
        entity.version = config.version();
        entity.transferType = config.transferType();
        entity.endpoint = config.endpoint();
        entity.parameters = config.parameters();
        entity.status = config.status();
        entity.createdBy = config.createdBy();
        entity.createdAt = config.createdAt();
        entity.updatedAt = config.updatedAt();
        return entity;
    }

    public TransferConfigVersion toDomain() {
        return new TransferConfigVersion(transferConfigId, version, transferType, endpoint, parameters,
                status, createdBy, createdAt, updatedAt);
    }
}
