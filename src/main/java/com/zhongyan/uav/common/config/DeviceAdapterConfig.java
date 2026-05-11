package com.zhongyan.uav.common.config;

import com.zhongyan.uav.device.infrastructure.mock.MockCameraAdapter;
import com.zhongyan.uav.device.infrastructure.mock.MockClockSyncAdapter;
import com.zhongyan.uav.device.infrastructure.mock.MockDeviceCommandExecutor;
import com.zhongyan.uav.device.infrastructure.mock.MockModelAdapter;
import com.zhongyan.uav.device.infrastructure.mock.MockTransferAdapter;
import com.zhongyan.uav.device.port.CameraAdapter;
import com.zhongyan.uav.device.port.ClockSyncAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.ModelAdapter;
import com.zhongyan.uav.device.port.TransferAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "bms.device", name = "mode", havingValue = "mock", matchIfMissing = true)
public class DeviceAdapterConfig {
    @Bean
    public DeviceCommandExecutor deviceCommandExecutor() {
        return new MockDeviceCommandExecutor();
    }

    @Bean
    public CameraAdapter cameraAdapter(DeviceCommandExecutor deviceCommandExecutor) {
        return new MockCameraAdapter(deviceCommandExecutor);
    }

    @Bean
    public ModelAdapter modelAdapter(DeviceCommandExecutor deviceCommandExecutor) {
        return new MockModelAdapter(deviceCommandExecutor);
    }

    @Bean
    public TransferAdapter transferAdapter(DeviceCommandExecutor deviceCommandExecutor) {
        return new MockTransferAdapter(deviceCommandExecutor);
    }

    @Bean
    public ClockSyncAdapter clockSyncAdapter(DeviceCommandExecutor deviceCommandExecutor) {
        return new MockClockSyncAdapter(deviceCommandExecutor);
    }
}
