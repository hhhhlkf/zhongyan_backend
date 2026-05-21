package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.domain.TaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent read tool for task status, attempts, commands, and recent events.
 */
public class TaskQueryTool extends AbstractAgentTool {
    private final TaskRepository taskRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final TaskCommandRepository taskCommandRepository;

    public TaskQueryTool(TaskRepository taskRepository,
                         TaskEventRepository taskEventRepository,
                         TaskAttemptRepository taskAttemptRepository,
                         TaskCommandRepository taskCommandRepository) {
        super("task.query", "Query task status, attempts, commands, and events.",
                AgentToolInputSchema.of(Set.of(), Map.of(
                        "taskId", "Optional task identifier.",
                        "missionId", "Optional mission identifier.",
                        "status", "Optional task status filter.",
                        "includeEvents", "Whether to include recent task events.",
                        "limit", "Maximum number of tasks to return.")),
                ToolRiskLevel.LOW,
                Set.of("AGENT_CHAT"));
        this.taskRepository = taskRepository;
        this.taskEventRepository = taskEventRepository;
        this.taskAttemptRepository = taskAttemptRepository;
        this.taskCommandRepository = taskCommandRepository;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        String taskId = optionalText(input, "taskId");
        if (taskId != null && !taskId.isBlank()) {
            return taskRepository.findById(taskId)
                    .map(task -> AgentToolResult.success(Map.of(
                            "tasks", List.of(toMap(task)),
                            "attempts", attempts(task.taskId()),
                            "commands", commands(task.taskId()),
                            "events", events(task.taskId(), input))))
                    .orElseGet(() -> AgentToolResult.success(Map.of("tasks", List.of(), "notFound", taskId)));
        }
        List<Task> tasks = selectTasks(input);
        int limit = intValue(input, "limit", 20);
        return AgentToolResult.success(Map.of("tasks", tasks.stream()
                .limit(Math.max(limit, 1))
                .map(this::toMap)
                .toList()));
    }

    private List<Task> selectTasks(Map<String, Object> input) {
        String missionId = optionalText(input, "missionId");
        if (missionId != null && !missionId.isBlank()) {
            return taskRepository.findByMissionId(missionId);
        }
        String status = optionalText(input, "status");
        if (status != null && !status.isBlank()) {
            return taskRepository.findByStatus(TaskStatus.valueOf(status.toUpperCase()));
        }
        return List.of();
    }

    private Map<String, Object> toMap(Task task) {
        return Map.of(
                "taskId", task.taskId(),
                "missionId", task.missionId(),
                "taskType", task.taskType().name(),
                "status", task.status().name(),
                "priority", task.priority(),
                "progress", task.progress(),
                "createdAt", task.createdAt().toString());
    }

    private List<Map<String, Object>> attempts(String taskId) {
        return taskAttemptRepository.findByTaskId(taskId).stream()
                .map(attempt -> Map.<String, Object>of(
                        "attemptId", attempt.attemptId(),
                        "attemptNo", attempt.attemptNo(),
                        "result", attempt.result().name(),
                        "executorNode", defaultText(attempt.executorNode())))
                .toList();
    }

    private List<Map<String, Object>> commands(String taskId) {
        return taskCommandRepository.findByTaskId(taskId).stream()
                .map(command -> Map.<String, Object>of(
                        "commandId", command.commandId(),
                        "commandType", command.commandType().name(),
                        "status", command.status().name(),
                        "riskLevel", command.riskLevel().name()))
                .toList();
    }

    private List<Map<String, Object>> events(String taskId, Map<String, Object> input) {
        if (!booleanValue(input, "includeEvents", true)) {
            return List.of();
        }
        int limit = intValue(input, "eventLimit", 20);
        return taskEventRepository.findByTaskId(taskId).stream()
                .limit(Math.max(limit, 1))
                .map(event -> Map.<String, Object>of(
                        "eventId", event.eventId(),
                        "eventType", event.eventType().name(),
                        "occurredAt", event.createdAt().toString(),
                        "payload", event.payload()))
                .toList();
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
