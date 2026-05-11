package com.zhongyan.uav.telemetry.api.response;

import java.time.Instant;

public record UavTelemetryView(
        String telemetryId,
        String uavId,
        Double latitude,
        Double longitude,
        Double altitudeMeters,
        Double speedMetersPerSecond,
        Double headingDegrees,
        String status,
        Instant reportedAt) {
    /**
     * 构建遥测占位视图，避免 API 空壳接入真实遥测存储。
     */
    public static UavTelemetryView placeholder(String telemetryId, String uavId,
                                               Double latitude, Double longitude, Double altitudeMeters) {
        return new UavTelemetryView(telemetryId, uavId, latitude, longitude, altitudeMeters,
                null, null, "PLACEHOLDER", Instant.now());
    }
}
