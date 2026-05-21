package com.zhongyan.uav.telemetry.application;

import com.zhongyan.uav.event.application.OutboxPublishService;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.domain.UavTelemetryRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class UavTelemetryIngestService {
    private final UavTelemetryRepository telemetryRepository;
    private final OutboxPublishService outboxPublishService;
    private final Clock clock;

    public UavTelemetryIngestService(UavTelemetryRepository telemetryRepository,
                                     OutboxPublishService outboxPublishService) {
        this(telemetryRepository, outboxPublishService, Clock.systemUTC());
    }

    public UavTelemetryIngestService(UavTelemetryRepository telemetryRepository,
                                     OutboxPublishService outboxPublishService,
                                     Clock clock) {
        this.telemetryRepository = Objects.requireNonNull(telemetryRepository, "telemetryRepository must not be null");
        this.outboxPublishService = Objects.requireNonNull(outboxPublishService, "outboxPublishService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public UavTelemetry ingest(IngestTelemetryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant recordedAt = command.reportedAt() == null ? clock.instant() : command.reportedAt();
        UavTelemetry telemetry = UavTelemetry.record("telemetry-" + UUID.randomUUID(), command.uavId(),
                command.missionId(), command.taskId(), recordedAt, command.latitude(), command.longitude(),
                command.altitudeMeters(), command.headingDegrees(), command.speedMetersPerSecond(),
                enrichRawPayload(command));
        UavTelemetry saved = telemetryRepository.save(telemetry);
        outboxPublishService.enqueue("UAV_TELEMETRY", saved.uavId(), EventType.UAV_TELEMETRY,
                eventPayload(saved), Map.of("source", "telemetry-ingest"));
        return saved;
    }

    private Map<String, Object> enrichRawPayload(IngestTelemetryCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (command.rawPayload() != null) {
            payload.putAll(command.rawPayload());
        }
        payload.putIfAbsent("uavId", command.uavId());
        putIfPresent(payload, "missionId", command.missionId());
        putIfPresent(payload, "taskId", command.taskId());
        putIfPresent(payload, "latitude", command.latitude());
        putIfPresent(payload, "longitude", command.longitude());
        putIfPresent(payload, "altitudeMeters", command.altitudeMeters());
        putIfPresent(payload, "speedMetersPerSecond", command.speedMetersPerSecond());
        putIfPresent(payload, "headingDegrees", command.headingDegrees());
        putIfPresent(payload, "reportedAt", command.reportedAt() == null ? null : command.reportedAt().toString());
        return payload;
    }

    private Map<String, Object> eventPayload(UavTelemetry telemetry) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("telemetryId", telemetry.telemetryId());
        payload.put("uavId", telemetry.uavId());
        putIfPresent(payload, "missionId", telemetry.missionId());
        putIfPresent(payload, "taskId", telemetry.taskId());
        payload.put("recordedAt", telemetry.recordedAt().toString());
        putIfPresent(payload, "latitude", telemetry.latitude());
        putIfPresent(payload, "longitude", telemetry.longitude());
        putIfPresent(payload, "altitudeMeters", telemetry.altitudeMeters());
        putIfPresent(payload, "speedMetersPerSecond", telemetry.speedMetersPerSecond());
        putIfPresent(payload, "headingDegrees", telemetry.headingDegrees());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }
}
