package com.zhongyan.uav.mission.application;

import com.zhongyan.uav.mission.domain.MissionRegion;

public record CreateMissionInput(
        String name,
        String scenarioType,
        MissionRegion region,
        int priority,
        String createdBy,
        String description) {
}
