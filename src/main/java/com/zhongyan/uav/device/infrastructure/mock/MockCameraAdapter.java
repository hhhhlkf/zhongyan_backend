package com.zhongyan.uav.device.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.device.domain.CaptureParameters;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.port.CameraAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Clock;
import java.util.Map;

public class MockCameraAdapter implements CameraAdapter {
    private final DeviceCommandExecutor executor;
    private final Clock clock;

    public MockCameraAdapter(DeviceCommandExecutor executor) {
        this(executor, Clock.systemUTC());
    }

    public MockCameraAdapter(DeviceCommandExecutor executor, Clock clock) {
        this.executor = executor;
        this.clock = clock;
    }

    @Override
    public DeviceHealth check(CameraConfigVersion config) {
        return DeviceHealth.up(config.cameraConfigId(), "mock camera reachable",
                Map.of("cameraType", config.cameraType(), "version", config.version()), clock.instant());
    }

    @Override
    public DeviceCommandResult start(CameraConfigVersion config, CaptureParameters parameters,
                                     String taskId, String commandId) {
        return executor.execute(payload(commandId, taskId, config.cameraConfigId(),
                TaskCommandType.START_CAPTURE, parameters.values()));
    }

    @Override
    public DeviceCommandResult stop(CameraConfigVersion config, String taskId, String commandId) {
        return executor.execute(payload(commandId, taskId, config.cameraConfigId(),
                TaskCommandType.STOP_DEVICE, Map.of("cameraType", config.cameraType())));
    }

    private DeviceCommandPayload payload(String commandId, String taskId, String deviceId,
                                         TaskCommandType commandType, Map<String, Object> parameters) {
        return new DeviceCommandPayload(commandId, taskId, deviceId, commandType,
                DeviceProtocol.MOCK, parameters, null);
    }
}
