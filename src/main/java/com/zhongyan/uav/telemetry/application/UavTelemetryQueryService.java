package com.zhongyan.uav.telemetry.application;

import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.domain.UavTelemetryRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class UavTelemetryQueryService {
    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 5000;

    private final UavTelemetryRepository telemetryRepository;
    private final Clock clock;

    public UavTelemetryQueryService(UavTelemetryRepository telemetryRepository) {
        this(telemetryRepository, Clock.systemUTC());
    }

    public UavTelemetryQueryService(UavTelemetryRepository telemetryRepository, Clock clock) {
        this.telemetryRepository = Objects.requireNonNull(telemetryRepository, "telemetryRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public UavTelemetry latest(String uavId) {
        requireText(uavId, "uavId");
        return telemetryRepository.findLatestByUavId(uavId)
                .orElseThrow(() -> new NoSuchElementException("telemetry not found for uavId " + uavId));
    }

    public List<UavTelemetry> range(String uavId, Instant from, Instant to, Integer limit) {
        requireText(uavId, "uavId");
        Instant effectiveFrom = from == null ? Instant.EPOCH : from;
        Instant effectiveTo = to == null ? clock.instant() : to;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        return telemetryRepository.findByUavIdAndRecordedAtBetween(uavId, effectiveFrom,
                effectiveTo, normalizeLimit(limit));
    }

    public List<UavTelemetry> track(String uavId, String missionId, Integer limit) {
        requireText(uavId, "uavId");
        return telemetryRepository.findTrack(uavId, missionId, normalizeLimit(limit));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
