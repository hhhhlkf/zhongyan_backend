package com.zhongyan.uav.telemetry.application;

import java.time.Instant;
import java.util.Map;

public record IngestTelemetryCommand(
        String uavId,
        String missionId,
        String taskId,
        Double latitude,
        Double longitude,
        Double altitudeMeters,
        Double speedMetersPerSecond,
        Double headingDegrees,
        Instant reportedAt,
        Map<String, Object> rawPayload) {
}
