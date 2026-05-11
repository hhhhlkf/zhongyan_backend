package com.zhongyan.uav.task.application;

import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskRepository;

import java.time.Clock;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class TaskCommandApplicationService {
    private final TaskRepository taskRepository;
    private final TaskCommandRepository taskCommandRepository;
    private final TaskEventRepository taskEventRepository;
    private final Clock clock;

    /**
     * 使用系统时钟创建 Task 命令应用服务。
     */
    public TaskCommandApplicationService(TaskRepository taskRepository,
                                         TaskCommandRepository taskCommandRepository,
                                         TaskEventRepository taskEventRepository) {
        this(taskRepository, taskCommandRepository, taskEventRepository, Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建 Task 命令应用服务，测试时可传入固定时钟。
     */
    public TaskCommandApplicationService(TaskRepository taskRepository,
                                         TaskCommandRepository taskCommandRepository,
                                         TaskEventRepository taskEventRepository,
                                         Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskCommandRepository = Objects.requireNonNull(taskCommandRepository, "taskCommandRepository must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository, "taskEventRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 创建 TaskCommand；如果幂等键已存在，直接返回已有命令。
     */
    public TaskCommand createCommand(String taskId, CreateTaskCommandInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));

        return taskCommandRepository.findByIdempotencyKey(input.idempotencyKey())
                .orElseGet(() -> createNewCommand(task, input));
    }

    /**
     * 创建新的 TaskCommand 并记录命令创建事件。
     */
    private TaskCommand createNewCommand(Task task, CreateTaskCommandInput input) {
        String deviceId = input.deviceId() == null ? task.deviceId() : input.deviceId();
        TaskCommand command = TaskCommand.create(nextId("command"), task.taskId(), task.missionId(),
                deviceId, input.commandType(), input.payload(), input.idempotencyKey(),
                input.requestedBy(), input.riskLevel(), input.reason(), clock.instant());
        TaskCommand savedCommand = taskCommandRepository.save(command);
        TaskEvent event = new TaskEvent(nextId("event"), task.taskId(), TaskEventType.COMMAND_CREATED,
                task.status(), task.status(), Map.of("commandId", savedCommand.commandId(),
                "commandType", savedCommand.commandType().name(),
                "commandStatus", savedCommand.status().name(),
                "requiresApproval", savedCommand.requiresApproval()), clock.instant());
        taskEventRepository.save(event);
        return savedCommand;
    }

    /**
     * 生成带领域前缀的本地唯一编号。
     */
    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
