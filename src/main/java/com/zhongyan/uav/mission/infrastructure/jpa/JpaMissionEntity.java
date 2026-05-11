package com.zhongyan.uav.mission.infrastructure.jpa;

import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRegion;
import com.zhongyan.uav.mission.domain.MissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "mission")
public class JpaMissionEntity {
    @Id
    @Column(name = "mission_id", length = 80)
    private String missionId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "scenario_type", nullable = false, length = 80)
    private String scenarioType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "region", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> region = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MissionStatus status;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "description")
    private String description;

    protected JpaMissionEntity() {
    }

    public static JpaMissionEntity fromDomain(Mission mission) {
        JpaMissionEntity entity = new JpaMissionEntity();
        entity.missionId = mission.missionId();
        entity.name = mission.name();
        entity.scenarioType = mission.scenarioType();
        entity.region = regionToMap(mission.region());
        entity.status = mission.status();
        entity.priority = mission.priority();
        entity.createdBy = mission.createdBy();
        entity.createdAt = mission.createdAt();
        entity.startedAt = mission.startedAt();
        entity.endedAt = mission.endedAt();
        entity.description = mission.description();
        return entity;
    }

    public Mission toDomain() {
        return new Mission(missionId, name, scenarioType, mapToRegion(region), status, priority,
                createdBy, createdAt, startedAt, endedAt, description);
    }

    private static Map<String, Object> regionToMap(MissionRegion region) {
        if (region == null) {
            return Map.of();
        }
        Map<String, Object> values = new HashMap<>();
        putIfNotNull(values, "administrativeCode", region.administrativeCode());
        putIfNotNull(values, "administrativeName", region.administrativeName());
        putIfNotNull(values, "centerLongitude", region.centerLongitude());
        putIfNotNull(values, "centerLatitude", region.centerLatitude());
        putIfNotNull(values, "boundaryGeoJson", region.boundaryGeoJson());
        return values;
    }

    private static MissionRegion mapToRegion(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return new MissionRegion(
                stringValue(values.get("administrativeCode")),
                stringValue(values.get("administrativeName")),
                doubleValue(values.get("centerLongitude")),
                doubleValue(values.get("centerLatitude")),
                stringValue(values.get("boundaryGeoJson")));
    }

    private static void putIfNotNull(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
