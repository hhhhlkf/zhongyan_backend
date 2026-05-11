package com.zhongyan.uav.device.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.domain.TransferParameters;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.TransferAdapter;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Clock;
import java.util.Map;

public class MockTransferAdapter implements TransferAdapter {
    private final DeviceCommandExecutor executor;
    private final Clock clock;

    public MockTransferAdapter(DeviceCommandExecutor executor) {
        this(executor, Clock.systemUTC());
    }

    public MockTransferAdapter(DeviceCommandExecutor executor, Clock clock) {
        this.executor = executor;
        this.clock = clock;
    }

    @Override
    public DeviceHealth check(TransferConfigVersion config) {
        return DeviceHealth.up(config.transferConfigId(), "mock transfer reachable",
                Map.of("transferType", config.transferType(), "version", config.version()), clock.instant());
    }

    @Override
    public DeviceCommandResult start(TransferConfigVersion config, TransferParameters parameters,
                                     String taskId, String commandId) {
        return executor.execute(payload(commandId, taskId, config.transferConfigId(),
                TaskCommandType.START_TRANSFER, parameters.values()));
    }

    @Override
    public DeviceCommandResult stop(TransferConfigVersion config, String taskId, String commandId) {
        return executor.execute(payload(commandId, taskId, config.transferConfigId(),
                TaskCommandType.STOP_DEVICE, Map.of("transferType", config.transferType())));
    }

    private DeviceCommandPayload payload(String commandId, String taskId, String deviceId,
                                         TaskCommandType commandType, Map<String, Object> parameters) {
        return new DeviceCommandPayload(commandId, taskId, deviceId, commandType,
                DeviceProtocol.MOCK, parameters, null);
    }
}
