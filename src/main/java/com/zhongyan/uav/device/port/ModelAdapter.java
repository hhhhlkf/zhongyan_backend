package com.zhongyan.uav.device.port;

import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.ProcessParameters;

public interface ModelAdapter {
    DeviceHealth check(ModelConfigVersion config);

    DeviceCommandResult start(ModelConfigVersion config, ProcessParameters parameters, String taskId, String commandId);

    DeviceCommandResult stop(ModelConfigVersion config, String taskId, String commandId);
}
