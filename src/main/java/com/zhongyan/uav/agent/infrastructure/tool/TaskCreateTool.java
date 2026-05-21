package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.task.application.CreateTaskInput;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent planning tool that creates draft Tasks through the Task application service.
 */
public class TaskCreateTool extends AbstractAgentTool {
    private final TaskApplicationService taskApplicationService;

    public TaskCreateTool(TaskApplicationService taskApplicationService) {
        super("task.create", "Create a draft task plan through application services.",
                AgentToolInputSchema.of(Set.of("missionId", "taskType"), Map.of(
                        "missionId", "Mission identifier that owns the task.",
                        "taskType", "Task type to create.",
                        "priority", "Optional task priority.",
                        "deviceId", "Optional target device.",
                        "inputAssetIds", "Optional input asset identifiers.",
                        "parameters", "Structured task parameters.")),
                ToolRiskLevel.MEDIUM,
                Set.of("AGENT_TASK_PLAN"));
        this.taskApplicationService = taskApplicationService;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        Task task = taskApplicationService.createTask(new CreateTaskInput(
                text(input, "missionId"),
                TaskType.valueOf(text(input, "taskType").toUpperCase()),
                intValue(input, "priority", 1),
                optionalText(input, "deviceId"),
                optionalText(input, "modelId"),
                mapValue(input, "parameters"),
                stringList(input.get("inputAssetIds")),
                context.requestedBy()));
        return AgentToolResult.success(Map.of(
                "taskId", task.taskId(),
                "missionId", task.missionId(),
                "taskType", task.taskType().name(),
                "status", task.status().name(),
                "inputAssetIds", task.inputAssetIds()));
    }
}
