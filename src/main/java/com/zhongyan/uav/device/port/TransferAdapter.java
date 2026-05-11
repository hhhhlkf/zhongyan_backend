package com.zhongyan.uav.device.port;

import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.TransferParameters;

public interface TransferAdapter {
    DeviceHealth check(TransferConfigVersion config);

    DeviceCommandResult start(TransferConfigVersion config, TransferParameters parameters, String taskId, String commandId);

    DeviceCommandResult stop(TransferConfigVersion config, String taskId, String commandId);
}
