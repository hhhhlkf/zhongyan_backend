package com.zhongyan.uav.device.port;

import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.device.domain.CaptureParameters;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;

public interface CameraAdapter {
    DeviceHealth check(CameraConfigVersion config);

    DeviceCommandResult start(CameraConfigVersion config, CaptureParameters parameters, String taskId, String commandId);

    DeviceCommandResult stop(CameraConfigVersion config, String taskId, String commandId);
}
