package com.zhongyan.uav.task.application;

import com.zhongyan.uav.task.domain.TaskType;

import java.util.List;
import java.util.Map;

public record CreateTaskInput(
        String missionId,
        TaskType taskType,
        int priority,
        String deviceId,
        String modelId,
        Map<String, Object> configSnapshot,
        List<String> inputAssetIds,
        String createdBy) {
}
