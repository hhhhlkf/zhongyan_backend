package com.zhongyan.uav.mission.api.response;

import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionStatus;

import java.time.Instant;

public record MissionView(
        String missionId,
        String name,
        String scenarioType,
        MissionStatus status,
        int priority,
        String createdBy,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        String description) {
    /**
     * 将 Mission 领域对象转换为 API 响应视图。
     */
    public static MissionView from(Mission mission) {
        return new MissionView(mission.missionId(), mission.name(), mission.scenarioType(),
                mission.status(), mission.priority(), mission.createdBy(), mission.createdAt(),
                mission.startedAt(), mission.endedAt(), mission.description());
    }
}
