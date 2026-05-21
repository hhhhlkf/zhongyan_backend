package com.zhongyan.uav.telemetry.api.response;

import com.zhongyan.uav.telemetry.domain.UavTelemetry;

import java.util.List;
import java.util.stream.Collectors;

public record UavTrackView(
        String uavId,
        String missionId,
        List<UavTelemetryView> points) {
    public static UavTrackView fromTelemetry(String uavId, String missionId, List<UavTelemetry> telemetry) {
        return new UavTrackView(uavId, missionId, telemetry.stream()
                .map(UavTelemetryView::fromDomain)
                .collect(Collectors.toList()));
    }
}
