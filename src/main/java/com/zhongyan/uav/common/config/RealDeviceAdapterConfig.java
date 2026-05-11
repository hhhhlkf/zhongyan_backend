package com.zhongyan.uav.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.device.infrastructure.http.HttpDeviceCommandExecutor;
import com.zhongyan.uav.device.infrastructure.ssh.SshCameraAdapter;
import com.zhongyan.uav.device.infrastructure.ssh.SshDeviceCommandExecutor;
import com.zhongyan.uav.device.infrastructure.ssh.SshModelAdapter;
import com.zhongyan.uav.device.infrastructure.ssh.SshTransferAdapter;
import com.zhongyan.uav.device.port.CameraAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.ModelAdapter;
import com.zhongyan.uav.device.port.TransferAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(prefix = "bms.device", name = "mode", havingValue = "real")
public class RealDeviceAdapterConfig {
    @Bean
    @Primary
    public SshDeviceCommandExecutor sshDeviceCommandExecutor() {
        return new SshDeviceCommandExecutor();
    }

    @Bean
    public HttpDeviceCommandExecutor httpDeviceCommandExecutor(ObjectMapper objectMapper) {
        return new HttpDeviceCommandExecutor(HttpClient.newHttpClient(), objectMapper,
                java.time.Clock.systemUTC());
    }

    @Bean
    public CameraAdapter sshCameraAdapter(DeviceCommandExecutor sshDeviceCommandExecutor) {
        return new SshCameraAdapter(sshDeviceCommandExecutor);
    }

    @Bean
    public ModelAdapter sshModelAdapter(DeviceCommandExecutor sshDeviceCommandExecutor) {
        return new SshModelAdapter(sshDeviceCommandExecutor);
    }

    @Bean
    public TransferAdapter sshTransferAdapter(DeviceCommandExecutor sshDeviceCommandExecutor) {
        return new SshTransferAdapter(sshDeviceCommandExecutor);
    }
}
