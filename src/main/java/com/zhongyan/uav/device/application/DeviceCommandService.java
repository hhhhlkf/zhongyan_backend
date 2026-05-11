package com.zhongyan.uav.device.application;

import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class DeviceCommandService {
    private final TaskCommandRepository taskCommandRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskApplicationService taskApplicationService;
    private final DeviceCommandExecutor deviceCommandExecutor;
    private final Clock clock;

    public DeviceCommandService(TaskCommandRepository taskCommandRepository,
                                TaskEventRepository taskEventRepository,
                                TaskApplicationService taskApplicationService,
                                DeviceCommandExecutor deviceCommandExecutor) {
        this(taskCommandRepository, taskEventRepository, taskApplicationService,
                deviceCommandExecutor, Clock.systemUTC());
    }

    public DeviceCommandService(TaskCommandRepository taskCommandRepository,
                                TaskEventRepository taskEventRepository,
                                TaskApplicationService taskApplicationService,
                                DeviceCommandExecutor deviceCommandExecutor,
                                Clock clock) {
        this.taskCommandRepository = Objects.requireNonNull(taskCommandRepository, "taskCommandRepository must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository, "taskEventRepository must not be null");
        this.taskApplicationService = Objects.requireNonNull(taskApplicationService, "taskApplicationService must not be null");
        this.deviceCommandExecutor = Objects.requireNonNull(deviceCommandExecutor, "deviceCommandExecutor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DeviceCommandResult dispatch(String commandId) {
        TaskCommand command = taskCommandRepository.findById(commandId)
                .orElseThrow(() -> new NoSuchElementException("TaskCommand not found: " + commandId));
        if (command.status() == TaskCommandStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Command requires approval before dispatch");
        }
        if (command.status().isTerminal()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Command already finished: " + commandId);
        }

        TaskCommand dispatchedCommand = taskCommandRepository.save(command.markDispatched(clock.instant()));
        taskApplicationService.startTaskAttempt(dispatchedCommand.taskId(), "device-command-service");
        recordEvent(dispatchedCommand, TaskEventType.DISPATCHED,
                Map.of("commandId", dispatchedCommand.commandId(),
                        "commandType", dispatchedCommand.commandType().name(),
                        "deviceId", defaultText(dispatchedCommand.deviceId(), "mock-device")));

        DeviceCommandResult result = deviceCommandExecutor.execute(toPayload(dispatchedCommand));
        if (result.success()) {
            taskCommandRepository.save(dispatchedCommand.complete(result.completedAt()));
            taskApplicationService.completeTask(dispatchedCommand.taskId(), result.rawLogObjectKey());
            recordEvent(dispatchedCommand, TaskEventType.COMPLETED, resultPayload(result));
        } else {
            taskCommandRepository.save(dispatchedCommand.fail(result.completedAt()));
            taskApplicationService.failTask(dispatchedCommand.taskId(), result.exitCode(),
                    result.message(), result.rawLogObjectKey());
        }
        return result;
    }

    private DeviceCommandPayload toPayload(TaskCommand command) {
        return new DeviceCommandPayload(command.commandId(), command.taskId(),
                defaultText(command.deviceId(), "mock-device"), command.commandType(),
                DeviceProtocol.MOCK, command.payload(), null);
    }

    private Map<String, Object> resultPayload(DeviceCommandResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commandId", result.commandId());
        payload.put("deviceId", result.deviceId());
        payload.put("exitCode", result.exitCode());
        payload.put("message", defaultText(result.message(), ""));
        payload.put("rawLogObjectKey", result.rawLogObjectKey());
        return payload;
    }

    private void recordEvent(TaskCommand command, TaskEventType eventType, Map<String, Object> payload) {
        taskEventRepository.save(new TaskEvent("event-" + UUID.randomUUID(), command.taskId(),
                eventType, null, TaskStatus.RUNNING, payload, clock.instant()));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
