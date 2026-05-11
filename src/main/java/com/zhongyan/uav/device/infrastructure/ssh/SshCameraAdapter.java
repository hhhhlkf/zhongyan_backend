package com.zhongyan.uav.device.infrastructure.ssh;

import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.device.domain.CaptureParameters;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.infrastructure.DevicePayloads;
import com.zhongyan.uav.device.port.CameraAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class SshCameraAdapter implements CameraAdapter {
    private final DeviceCommandExecutor executor;
    private final Clock clock;

    public SshCameraAdapter(DeviceCommandExecutor executor) {
        this(executor, Clock.systemUTC());
    }

    public SshCameraAdapter(DeviceCommandExecutor executor, Clock clock) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public DeviceHealth check(CameraConfigVersion config) {
        DeviceCommandResult result = executor.execute(payload(config, TaskCommandType.START_CAPTURE,
                DevicePayloads.text(config.parameters(), "true", "check-command", "checkCommand"),
                Map.of(), "health-" + config.cameraConfigId(), "health-check"));
        return health(config.cameraConfigId(), result, Map.of("cameraType", config.cameraType(),
                "version", config.version()));
    }

    @Override
    public DeviceCommandResult start(CameraConfigVersion config, CaptureParameters parameters,
                                     String taskId, String commandId) {
        return executor.execute(payload(config, TaskCommandType.START_CAPTURE,
                DevicePayloads.text(config.parameters(), null, "start-command", "startCommand"),
                parameters.values(), taskId, commandId));
    }

    @Override
    public DeviceCommandResult stop(CameraConfigVersion config, String taskId, String commandId) {
        return executor.execute(payload(config, TaskCommandType.STOP_DEVICE,
                DevicePayloads.text(config.parameters(), null, "stop-command", "stopCommand"),
                Map.of(), taskId, commandId));
    }

    private DeviceCommandPayload payload(CameraConfigVersion config, TaskCommandType commandType,
                                         String command, Map<String, Object> commandParameters,
                                         String taskId, String commandId) {
        Map<String, Object> values = DevicePayloads.merge(config.parameters(), commandParameters,
                Map.of("command", command == null ? "" : command));
        return new DeviceCommandPayload(commandId, taskId, config.cameraConfigId(), commandType,
                DeviceProtocol.SSH, values, timeout(values));
    }

    private Duration timeout(Map<String, Object> values) {
        return DevicePayloads.duration(values, Duration.ofSeconds(30),
                "command-timeout-ms", "commandTimeoutMs", "timeoutMs");
    }

    private DeviceHealth health(String targetId, DeviceCommandResult result, Map<String, Object> details) {
        return result.success()
                ? DeviceHealth.up(targetId, result.message(), details, clock.instant())
                : DeviceHealth.down(targetId, result.message(), details, clock.instant());
    }
}
