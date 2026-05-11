package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.device.domain.CaptureParameters;
import com.zhongyan.uav.device.port.CameraAdapter;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class CaptureTaskExecutor extends AbstractDeviceTaskExecutor {
    private final CameraAdapter cameraAdapter;
    private final CameraConfigRepository cameraConfigRepository;

    public CaptureTaskExecutor(CameraAdapter cameraAdapter, CameraConfigRepository cameraConfigRepository) {
        this.cameraAdapter = Objects.requireNonNull(cameraAdapter, "cameraAdapter must not be null");
        this.cameraConfigRepository = Objects.requireNonNull(cameraConfigRepository, "cameraConfigRepository must not be null");
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.CAPTURE;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        TaskCommand command = context.command();
        CameraConfigVersion config = resolveConfig(task);
        return TaskExecutionResult.fromDeviceResult(cameraAdapter.start(config,
                new CaptureParameters(command.payload()), task.taskId(), command.commandId()));
    }

    private CameraConfigVersion resolveConfig(Task task) {
        Map<String, Object> snapshot = task.configSnapshot();
        String configId = text(snapshot, "cameraConfigId",
                task.deviceId() == null || task.deviceId().isBlank() ? "mock-camera-config" : task.deviceId());
        return cameraConfigRepository.findLatestByConfigId(configId)
                .orElseGet(() -> new CameraConfigVersion(configId, intValue(snapshot, "version", 1),
                        text(snapshot, "cameraType", "mock-camera"), map(snapshot.get("fov")),
                        snapshot, ConfigStatus.ACTIVE, task.createdBy(), task.createdAt(), Instant.now()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
