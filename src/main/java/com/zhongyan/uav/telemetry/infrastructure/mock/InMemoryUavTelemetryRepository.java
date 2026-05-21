package com.zhongyan.uav.telemetry.infrastructure.mock;

import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.domain.UavTelemetryRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryUavTelemetryRepository implements UavTelemetryRepository {
    private final Map<String, UavTelemetry> telemetry = new ConcurrentHashMap<>();

    @Override
    public UavTelemetry save(UavTelemetry value) {
        telemetry.put(value.telemetryId(), value);
        return value;
    }

    @Override
    public Optional<UavTelemetry> findLatestByUavId(String uavId) {
        return telemetry.values().stream()
                .filter(value -> value.uavId().equals(uavId))
                .max(Comparator.comparing(UavTelemetry::recordedAt));
    }

    @Override
    public List<UavTelemetry> findByUavIdAndRecordedAtBetween(String uavId, Instant from, Instant to, int limit) {
        return telemetry.values().stream()
                .filter(value -> value.uavId().equals(uavId))
                .filter(value -> !value.recordedAt().isBefore(from))
                .filter(value -> !value.recordedAt().isAfter(to))
                .sorted(Comparator.comparing(UavTelemetry::recordedAt))
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<UavTelemetry> findTrack(String uavId, String missionId, int limit) {
        return telemetry.values().stream()
                .filter(value -> value.uavId().equals(uavId))
                .filter(value -> missionId == null || missionId.isBlank() || missionId.equals(value.missionId()))
                .filter(value -> value.latitude() != null && value.longitude() != null)
                .sorted(Comparator.comparing(UavTelemetry::recordedAt))
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }
}
