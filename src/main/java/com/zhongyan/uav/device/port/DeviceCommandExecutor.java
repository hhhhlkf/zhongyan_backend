package com.zhongyan.uav.device.port;

import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;

public interface DeviceCommandExecutor {
    DeviceCommandResult execute(DeviceCommandPayload payload);
}
