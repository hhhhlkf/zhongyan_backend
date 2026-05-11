package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.ModelArtifact;
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
@Table(name = "model_artifact")
public class JpaModelArtifactEntity {
    @Id
    @Column(name = "artifact_id", length = 120)
    private String artifactId;

    @Column(name = "model_config_id", nullable = false, length = 120)
    private String modelConfigId;

    @Column(name = "artifact_type", nullable = false, length = 80)
    private String artifactType;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "checksum", length = 160)
    private String checksum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConfigStatus status;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JpaModelArtifactEntity() {
    }

    public static JpaModelArtifactEntity fromDomain(ModelArtifact artifact) {
        JpaModelArtifactEntity entity = new JpaModelArtifactEntity();
        entity.artifactId = artifact.artifactId();
        entity.modelConfigId = artifact.modelConfigId();
        entity.artifactType = artifact.artifactType();
        entity.objectKey = artifact.objectKey();
        entity.checksum = artifact.checksum();
        entity.metadata = artifact.metadata();
        entity.status = artifact.status();
        entity.createdBy = artifact.createdBy();
        entity.createdAt = artifact.createdAt();
        return entity;
    }

    public ModelArtifact toDomain() {
        return new ModelArtifact(artifactId, modelConfigId, artifactType, objectKey, checksum,
                metadata, status, createdBy, createdAt);
    }
}
