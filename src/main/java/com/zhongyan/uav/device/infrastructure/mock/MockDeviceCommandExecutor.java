package com.zhongyan.uav.device.infrastructure.mock;

import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;

import java.time.Clock;
import java.util.Map;

public class MockDeviceCommandExecutor implements DeviceCommandExecutor {
    private final Clock clock;

    public MockDeviceCommandExecutor() {
        this(Clock.systemUTC());
    }

    public MockDeviceCommandExecutor(Clock clock) {
        this.clock = clock;
    }

    @Override
    public DeviceCommandResult execute(DeviceCommandPayload payload) {
        return DeviceCommandResult.success(payload, "mock command executed",
                "mock " + payload.commandType() + " executed for " + payload.deviceId(),
                Map.of("protocol", payload.protocol().name(), "mock", true), clock.instant());
    }
}
