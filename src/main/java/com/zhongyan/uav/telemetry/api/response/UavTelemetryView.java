package com.zhongyan.uav.telemetry.api.response;

import com.zhongyan.uav.telemetry.domain.UavTelemetry;

import java.time.Instant;

public record UavTelemetryView(
        String telemetryId,
        String uavId,
        String missionId,
        String taskId,
        Double latitude,
        Double longitude,
        Double altitudeMeters,
        Double speedMetersPerSecond,
        Double headingDegrees,
        String status,
        Instant reportedAt) {
    public static UavTelemetryView fromDomain(UavTelemetry telemetry) {
        return new UavTelemetryView(telemetry.telemetryId(), telemetry.uavId(), telemetry.missionId(),
                telemetry.taskId(), telemetry.latitude(), telemetry.longitude(), telemetry.altitudeMeters(),
                telemetry.speedMetersPerSecond(), telemetry.headingDegrees(), "RECORDED",
                telemetry.recordedAt());
    }
}
