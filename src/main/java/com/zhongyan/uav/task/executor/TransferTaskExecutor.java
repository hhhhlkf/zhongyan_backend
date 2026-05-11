package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.device.domain.TransferParameters;
import com.zhongyan.uav.device.port.TransferAdapter;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class TransferTaskExecutor extends AbstractDeviceTaskExecutor {
    private final TransferAdapter transferAdapter;
    private final TransferConfigRepository transferConfigRepository;

    public TransferTaskExecutor(TransferAdapter transferAdapter, TransferConfigRepository transferConfigRepository) {
        this.transferAdapter = Objects.requireNonNull(transferAdapter, "transferAdapter must not be null");
        this.transferConfigRepository = Objects.requireNonNull(transferConfigRepository, "transferConfigRepository must not be null");
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.TRANSFER;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        TaskCommand command = context.command();
        TransferConfigVersion config = resolveConfig(task);
        return TaskExecutionResult.fromDeviceResult(transferAdapter.start(config,
                new TransferParameters(command.payload()), task.taskId(), command.commandId()));
    }

    private TransferConfigVersion resolveConfig(Task task) {
        Map<String, Object> snapshot = task.configSnapshot();
        String configId = text(snapshot, "transferConfigId",
                task.deviceId() == null || task.deviceId().isBlank() ? "mock-transfer-config" : task.deviceId());
        return transferConfigRepository.findLatestByConfigId(configId)
                .orElseGet(() -> new TransferConfigVersion(configId, intValue(snapshot, "version", 1),
                        text(snapshot, "transferType", "mock-transfer"), map(snapshot.get("endpoint")),
                        snapshot, ConfigStatus.ACTIVE, task.createdBy(), task.createdAt(), Instant.now()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
