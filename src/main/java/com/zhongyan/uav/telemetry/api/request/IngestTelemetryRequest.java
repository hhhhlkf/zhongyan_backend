package com.zhongyan.uav.telemetry.api.request;

import java.time.Instant;
import java.util.Map;

public record IngestTelemetryRequest(
        String missionId,
        Double latitude,
        Double longitude,
        Double altitudeMeters,
        Double speedMetersPerSecond,
        Double headingDegrees,
        Instant reportedAt,
        Map<String, Object> rawPayload) {
}
