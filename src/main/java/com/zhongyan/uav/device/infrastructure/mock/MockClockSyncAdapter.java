package com.zhongyan.uav.device.infrastructure.mock;

import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.port.ClockSyncAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Instant;
import java.util.Map;

public class MockClockSyncAdapter implements ClockSyncAdapter {
    private final DeviceCommandExecutor executor;

    public MockClockSyncAdapter(DeviceCommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public DeviceCommandResult sync(String deviceId, Instant expectedTime, String taskId, String commandId) {
        return executor.execute(new DeviceCommandPayload(commandId, taskId, deviceId,
                TaskCommandType.UPDATE_DEVICE_COMMAND, DeviceProtocol.MOCK,
                Map.of("expectedTime", expectedTime == null ? Instant.now().toString() : expectedTime.toString()),
                null));
    }
}
