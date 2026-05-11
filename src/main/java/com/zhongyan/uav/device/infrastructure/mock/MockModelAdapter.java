package com.zhongyan.uav.device.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.domain.ProcessParameters;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.ModelAdapter;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Clock;
import java.util.Map;

public class MockModelAdapter implements ModelAdapter {
    private final DeviceCommandExecutor executor;
    private final Clock clock;

    public MockModelAdapter(DeviceCommandExecutor executor) {
        this(executor, Clock.systemUTC());
    }

    public MockModelAdapter(DeviceCommandExecutor executor, Clock clock) {
        this.executor = executor;
        this.clock = clock;
    }

    @Override
    public DeviceHealth check(ModelConfigVersion config) {
        return DeviceHealth.up(config.modelConfigId(), "mock model reachable",
                Map.of("modelType", config.modelType(), "runtimeType", config.runtimeType(),
                        "version", config.version()), clock.instant());
    }

    @Override
    public DeviceCommandResult start(ModelConfigVersion config, ProcessParameters parameters,
                                     String taskId, String commandId) {
        return executor.execute(payload(commandId, taskId, config.modelConfigId(),
                TaskCommandType.START_PROCESS, parameters.values()));
    }

    @Override
    public DeviceCommandResult stop(ModelConfigVersion config, String taskId, String commandId) {
        return executor.execute(payload(commandId, taskId, config.modelConfigId(),
                TaskCommandType.STOP_PROCESS, Map.of("modelType", config.modelType())));
    }

    private DeviceCommandPayload payload(String commandId, String taskId, String deviceId,
                                         TaskCommandType commandType, Map<String, Object> parameters) {
        return new DeviceCommandPayload(commandId, taskId, deviceId, commandType,
                DeviceProtocol.MOCK, parameters, null);
    }
}
