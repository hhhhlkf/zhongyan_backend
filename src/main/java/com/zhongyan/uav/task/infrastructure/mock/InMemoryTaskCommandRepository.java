package com.zhongyan.uav.task.infrastructure.mock;

import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskCommandStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTaskCommandRepository implements TaskCommandRepository {
    private final Map<String, TaskCommand> commands = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>();

    /**
     * 将 TaskCommand 保存到内存 Map，并维护幂等键索引。
     */
    @Override
    public TaskCommand save(TaskCommand command) {
        commands.put(command.commandId(), command);
        idempotencyIndex.put(command.idempotencyKey(), command.commandId());
        return command;
    }

    /**
     * 从内存中按命令编号查找命令。
     */
    @Override
    public Optional<TaskCommand> findById(String commandId) {
        return Optional.ofNullable(commands.get(commandId));
    }

    /**
     * 从内存幂等索引中查找命令。
     */
    @Override
    public Optional<TaskCommand> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(idempotencyIndex.get(idempotencyKey))
                .map(commands::get);
    }

    /**
     * 从内存中查询某个 Task 的全部命令。
     */
    @Override
    public List<TaskCommand> findByTaskId(String taskId) {
        return commands.values().stream()
                .filter(command -> command.taskId().equals(taskId))
                .sorted(Comparator.comparing(TaskCommand::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 从内存中按状态筛选命令。
     */
    @Override
    public List<TaskCommand> findByStatus(TaskCommandStatus status) {
        return commands.values().stream()
                .filter(command -> command.status() == status)
                .sorted(Comparator.comparing(TaskCommand::createdAt))
                .collect(Collectors.toList());
    }
}
