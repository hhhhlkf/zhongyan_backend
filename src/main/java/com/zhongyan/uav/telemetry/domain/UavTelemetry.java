package com.zhongyan.uav.telemetry.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record UavTelemetry(
        String telemetryId,
        String uavId,
        String missionId,
        String taskId,
        Instant recordedAt,
        Double latitude,
        Double longitude,
        Double altitudeMeters,
        Double headingDegrees,
        Double speedMetersPerSecond,
        Map<String, Object> rawPayload) {
    public UavTelemetry {
        requireText(telemetryId, "telemetryId");
        requireText(uavId, "uavId");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        validateLatitude(latitude);
        validateLongitude(longitude);
        rawPayload = rawPayload == null ? Map.of() : Map.copyOf(rawPayload);
    }

    public static UavTelemetry record(String telemetryId, String uavId, String missionId, String taskId,
                                      Instant recordedAt, Double latitude, Double longitude,
                                      Double altitudeMeters, Double headingDegrees,
                                      Double speedMetersPerSecond, Map<String, Object> rawPayload) {
        return new UavTelemetry(telemetryId, uavId, blankToNull(missionId), blankToNull(taskId),
                recordedAt, latitude, longitude, altitudeMeters, headingDegrees,
                speedMetersPerSecond, rawPayload);
    }

    private static void validateLatitude(Double latitude) {
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
    }

    private static void validateLongitude(Double longitude) {
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
