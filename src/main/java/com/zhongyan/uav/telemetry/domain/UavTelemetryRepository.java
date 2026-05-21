package com.zhongyan.uav.telemetry.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UavTelemetryRepository {
    UavTelemetry save(UavTelemetry telemetry);

    Optional<UavTelemetry> findLatestByUavId(String uavId);

    List<UavTelemetry> findByUavIdAndRecordedAtBetween(String uavId, Instant from, Instant to, int limit);

    List<UavTelemetry> findTrack(String uavId, String missionId, int limit);
}
