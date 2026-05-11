package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.DeviceConfig;
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
@Table(name = "device_config")
public class JpaDeviceConfigEntity {
    @Id
    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "device_name", nullable = false, length = 200)
    private String deviceName;

    @Column(name = "device_type", nullable = false, length = 80)
    private String deviceType;

    @Column(name = "host", length = 255)
    private String host;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> connection = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> capabilities = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConfigStatus status;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaDeviceConfigEntity() {
    }

    public static JpaDeviceConfigEntity fromDomain(DeviceConfig config) {
        JpaDeviceConfigEntity entity = new JpaDeviceConfigEntity();
        entity.deviceId = config.deviceId();
        entity.deviceName = config.deviceName();
        entity.deviceType = config.deviceType();
        entity.host = config.host();
        entity.connection = config.connection();
        entity.capabilities = config.capabilities();
        entity.status = config.status();
        entity.createdBy = config.createdBy();
        entity.createdAt = config.createdAt();
        entity.updatedAt = config.updatedAt();
        return entity;
    }

    public DeviceConfig toDomain() {
        return new DeviceConfig(deviceId, deviceName, deviceType, host, connection, capabilities,
                status, createdBy, createdAt, updatedAt);
    }
}
