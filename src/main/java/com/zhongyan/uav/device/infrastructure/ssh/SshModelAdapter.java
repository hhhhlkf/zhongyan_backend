package com.zhongyan.uav.device.infrastructure.ssh;

import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceHealth;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.domain.ProcessParameters;
import com.zhongyan.uav.device.infrastructure.DevicePayloads;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.ModelAdapter;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class SshModelAdapter implements ModelAdapter {
    private final DeviceCommandExecutor executor;
    private final Clock clock;

    public SshModelAdapter(DeviceCommandExecutor executor) {
        this(executor, Clock.systemUTC());
    }

    public SshModelAdapter(DeviceCommandExecutor executor, Clock clock) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public DeviceHealth check(ModelConfigVersion config) {
        DeviceCommandResult result = executor.execute(payload(config, TaskCommandType.START_PROCESS,
                DevicePayloads.text(config.parameters(), "true", "check-command", "checkCommand"),
                Map.of(), "health-" + config.modelConfigId(), "health-check"));
        return result.success()
                ? DeviceHealth.up(config.modelConfigId(), result.message(), details(config), clock.instant())
                : DeviceHealth.down(config.modelConfigId(), result.message(), details(config), clock.instant());
    }

    @Override
    public DeviceCommandResult start(ModelConfigVersion config, ProcessParameters parameters,
                                     String taskId, String commandId) {
        return executor.execute(payload(config, TaskCommandType.START_PROCESS,
                DevicePayloads.text(config.parameters(), null, "start-command", "startCommand"),
                parameters.values(), taskId, commandId));
    }

    @Override
    public DeviceCommandResult stop(ModelConfigVersion config, String taskId, String commandId) {
        return executor.execute(payload(config, TaskCommandType.STOP_PROCESS,
                DevicePayloads.text(config.parameters(), null, "stop-command", "stopCommand"),
                Map.of(), taskId, commandId));
    }

    private DeviceCommandPayload payload(ModelConfigVersion config, TaskCommandType commandType,
                                         String command, Map<String, Object> commandParameters,
                                         String taskId, String commandId) {
        Map<String, Object> values = DevicePayloads.merge(config.parameters(), commandParameters,
                Map.of("command", command == null ? "" : command));
        return new DeviceCommandPayload(commandId, taskId, config.modelConfigId(), commandType,
                DeviceProtocol.SSH, values, timeout(values));
    }

    private Duration timeout(Map<String, Object> values) {
        return DevicePayloads.duration(values, Duration.ofSeconds(30),
                "command-timeout-ms", "commandTimeoutMs", "timeoutMs");
    }

    private Map<String, Object> details(ModelConfigVersion config) {
        return Map.of("modelType", config.modelType(), "runtimeType", config.runtimeType(),
                "version", config.version());
    }
}
