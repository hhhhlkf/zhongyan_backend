package com.zhongyan.uav.mission.api.request;

import com.zhongyan.uav.mission.application.CreateMissionInput;
import com.zhongyan.uav.mission.domain.MissionRegion;

public record CreateMissionRequest(
        String name,
        String scenarioType,
        MissionRegion region,
        int priority,
        String createdBy,
        String description) {
    /**
     * 将 API 请求转换为应用服务输入对象。
     */
    public CreateMissionInput toInput() {
        return new CreateMissionInput(name, scenarioType, region, priority,
                createdBy == null || createdBy.isBlank() ? "mock-user" : createdBy,
                description);
    }
}
