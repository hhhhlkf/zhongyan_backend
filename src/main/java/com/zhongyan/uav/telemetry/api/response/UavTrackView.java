package com.zhongyan.uav.telemetry.api.response;

import java.util.List;

public record UavTrackView(
        String uavId,
        String missionId,
        List<UavTelemetryView> points) {
    /**
     * 构建空轨迹视图，表示当前 API 空壳不读取历史遥测。
     */
    public static UavTrackView empty(String uavId, String missionId) {
        return new UavTrackView(uavId, missionId, List.of());
    }
}
