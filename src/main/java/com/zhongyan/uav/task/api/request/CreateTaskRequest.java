package com.zhongyan.uav.task.api.request;

import com.zhongyan.uav.task.application.CreateTaskInput;
import com.zhongyan.uav.task.domain.TaskType;

import java.util.List;
import java.util.Map;

public record CreateTaskRequest(
        TaskType taskType,
        int priority,
        String deviceId,
        String modelId,
        Map<String, Object> configSnapshot,
        List<String> inputAssetIds,
        String createdBy) {
    /**
     * 将 API 请求转换为应用服务输入对象。
     */
    public CreateTaskInput toInput(String missionId) {
        return new CreateTaskInput(missionId, taskType, priority, deviceId, modelId,
                configSnapshot, inputAssetIds,
                createdBy == null || createdBy.isBlank() ? "mock-user" : createdBy);
    }
}
