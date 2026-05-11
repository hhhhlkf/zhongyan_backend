package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.device.domain.ProcessParameters;
import com.zhongyan.uav.device.port.ModelAdapter;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class ProcessTaskExecutor extends AbstractDeviceTaskExecutor {
    private final ModelAdapter modelAdapter;
    private final ModelConfigRepository modelConfigRepository;

    public ProcessTaskExecutor(ModelAdapter modelAdapter, ModelConfigRepository modelConfigRepository) {
        this.modelAdapter = Objects.requireNonNull(modelAdapter, "modelAdapter must not be null");
        this.modelConfigRepository = Objects.requireNonNull(modelConfigRepository, "modelConfigRepository must not be null");
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.PROCESS;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        TaskCommand command = context.command();
        ModelConfigVersion config = resolveConfig(task);
        return TaskExecutionResult.fromDeviceResult(modelAdapter.start(config,
                new ProcessParameters(command.payload()), task.taskId(), command.commandId()));
    }

    private ModelConfigVersion resolveConfig(Task task) {
        Map<String, Object> snapshot = task.configSnapshot();
        String configId = text(snapshot, "modelConfigId",
                task.modelId() == null || task.modelId().isBlank() ? "mock-model-config" : task.modelId());
        return modelConfigRepository.findLatestByConfigId(configId)
                .orElseGet(() -> new ModelConfigVersion(configId, intValue(snapshot, "version", 1),
                        text(snapshot, "modelType", "mock-model"),
                        text(snapshot, "runtimeType", "mock-runtime"), snapshot, ConfigStatus.ACTIVE,
                        task.createdBy(), task.createdAt(), Instant.now()));
    }
}
