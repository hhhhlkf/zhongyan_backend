package com.zhongyan.uav.telemetry.infrastructure.jpa;

import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "uav_telemetry")
public class JpaUavTelemetryEntity {
    @Id
    @Column(name = "telemetry_id", length = 120)
    private String telemetryId;

    @Column(name = "uav_id", nullable = false, length = 120)
    private String uavId;

    @Column(name = "mission_id", length = 80)
    private String missionId;

    @Column(name = "task_id", length = 80)
    private String taskId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude_meters")
    private Double altitudeMeters;

    @Column(name = "heading_degrees")
    private Double headingDegrees;

    @Column(name = "speed_mps")
    private Double speedMetersPerSecond;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload = Map.of();

    protected JpaUavTelemetryEntity() {
    }

    public static JpaUavTelemetryEntity fromDomain(UavTelemetry telemetry) {
        JpaUavTelemetryEntity entity = new JpaUavTelemetryEntity();
        entity.telemetryId = telemetry.telemetryId();
        entity.uavId = telemetry.uavId();
        entity.missionId = telemetry.missionId();
        entity.taskId = telemetry.taskId();
        entity.recordedAt = telemetry.recordedAt();
        entity.latitude = telemetry.latitude();
        entity.longitude = telemetry.longitude();
        entity.altitudeMeters = telemetry.altitudeMeters();
        entity.headingDegrees = telemetry.headingDegrees();
        entity.speedMetersPerSecond = telemetry.speedMetersPerSecond();
        entity.rawPayload = telemetry.rawPayload();
        return entity;
    }

    public UavTelemetry toDomain() {
        return new UavTelemetry(telemetryId, uavId, missionId, taskId, recordedAt,
                latitude, longitude, altitudeMeters, headingDegrees, speedMetersPerSecond,
                rawPayload);
    }
}
