package com.zhongyan.uav.device.infrastructure.ssh;

import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.domain.TransferParameters;
import com.zhongyan.uav.device.infrastructure.DevicePayloads;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.TransferAdapter;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class SshTransferAdapter implements TransferAdapter {
    private final DeviceCommandExecutor executor;
    private final Clock clock;

    public SshTransferAdapter(DeviceCommandExecutor executor) {
        this(executor, Clock.systemUTC());
    }

    public SshTransferAdapter(DeviceCommandExecutor executor, Clock clock) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public DeviceHealth check(TransferConfigVersion config) {
        DeviceCommandResult result = executor.execute(payload(config, TaskCommandType.START_TRANSFER,
                DevicePayloads.text(config.parameters(), "true", "check-command", "checkCommand"),
                Map.of(), "health-" + config.transferConfigId(), "health-check"));
        Map<String, Object> details = Map.of("transferType", config.transferType(), "version", config.version());
        return result.success()
                ? DeviceHealth.up(config.transferConfigId(), result.message(), details, clock.instant())
                : DeviceHealth.down(config.transferConfigId(), result.message(), details, clock.instant());
    }

    @Override
    public DeviceCommandResult start(TransferConfigVersion config, TransferParameters parameters,
                                     String taskId, String commandId) {
        return executor.execute(payload(config, TaskCommandType.START_TRANSFER,
                DevicePayloads.text(config.parameters(), null, "start-command", "startCommand"),
                parameters.values(), taskId, commandId));
    }

    @Override
    public DeviceCommandResult stop(TransferConfigVersion config, String taskId, String commandId) {
        return executor.execute(payload(config, TaskCommandType.STOP_DEVICE,
                DevicePayloads.text(config.parameters(), null, "stop-command", "stopCommand"),
                Map.of(), taskId, commandId));
    }

    private DeviceCommandPayload payload(TransferConfigVersion config, TaskCommandType commandType,
                                         String command, Map<String, Object> commandParameters,
                                         String taskId, String commandId) {
        Map<String, Object> values = DevicePayloads.merge(config.endpoint(), config.parameters(),
                DevicePayloads.merge(commandParameters, Map.of("command", command == null ? "" : command)));
        return new DeviceCommandPayload(commandId, taskId, config.transferConfigId(), commandType,
                DeviceProtocol.SSH, values, timeout(values));
    }

    private Duration timeout(Map<String, Object> values) {
        return DevicePayloads.duration(values, Duration.ofSeconds(30),
                "command-timeout-ms", "commandTimeoutMs", "timeoutMs");
    }
}
