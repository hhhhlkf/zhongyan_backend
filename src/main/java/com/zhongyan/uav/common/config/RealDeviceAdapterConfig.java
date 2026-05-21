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
import org.springframework.beans.factory.annotation.Value;
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
    public SshDeviceCommandExecutor sshDeviceCommandExecutor(
            @Value("${bms.device.command.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${bms.device.command.retry-max-attempts:1}") int retryMaxAttempts,
            @Value("${bms.device.command.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${bms.device.command.circuit-failure-threshold:3}") int circuitFailureThreshold,
            @Value("${bms.device.command.circuit-open-duration-ms:30000}") long circuitOpenDurationMs) {
        return new SshDeviceCommandExecutor(java.time.Clock.systemUTC(), connectTimeoutMs, retryMaxAttempts,
                retryBackoffMs, circuitFailureThreshold, circuitOpenDurationMs);
    }

    @Bean
    public HttpDeviceCommandExecutor httpDeviceCommandExecutor(
            ObjectMapper objectMapper,
            @Value("${bms.device.command.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${bms.device.command.retry-max-attempts:1}") int retryMaxAttempts,
            @Value("${bms.device.command.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${bms.device.command.circuit-failure-threshold:3}") int circuitFailureThreshold,
            @Value("${bms.device.command.circuit-open-duration-ms:30000}") long circuitOpenDurationMs) {
        return new HttpDeviceCommandExecutor(HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(Math.max(1, connectTimeoutMs)))
                .build(), objectMapper, java.time.Clock.systemUTC(), retryMaxAttempts, retryBackoffMs,
                circuitFailureThreshold, circuitOpenDurationMs);
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
