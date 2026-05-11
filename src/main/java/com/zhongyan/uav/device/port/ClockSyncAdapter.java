package com.zhongyan.uav.device.port;

import com.zhongyan.uav.device.domain.DeviceCommandResult;

import java.time.Instant;

public interface ClockSyncAdapter {
    DeviceCommandResult sync(String deviceId, Instant expectedTime, String taskId, String commandId);
}
