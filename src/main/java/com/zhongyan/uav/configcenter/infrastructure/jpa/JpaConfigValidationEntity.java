package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ConfigValidation;
import com.zhongyan.uav.configcenter.domain.ConfigValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "config_validation")
public class JpaConfigValidationEntity {
    @Id
    @Column(name = "validation_id", length = 120)
    private String validationId;

    @Column(name = "config_type", nullable = false, length = 40)
    private String configType;

    @Column(name = "config_id", nullable = false, length = 120)
    private String configId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConfigValidationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errors", nullable = false, columnDefinition = "jsonb")
    private List<String> errors = List.of();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JpaConfigValidationEntity() {
    }

    public static JpaConfigValidationEntity fromDomain(ConfigValidation validation) {
        JpaConfigValidationEntity entity = new JpaConfigValidationEntity();
        entity.validationId = validation.validationId();
        entity.configType = validation.configType();
        entity.configId = validation.configId();
        entity.status = validation.status();
        entity.errors = validation.errors();
        entity.createdAt = validation.createdAt();
        return entity;
    }

    public ConfigValidation toDomain() {
        return new ConfigValidation(validationId, configType, configId, status, errors, createdAt);
    }
}
