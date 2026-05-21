package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskEventRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent read tool for diagnostic task events and attempt log object keys.
 */
public class DiagnosisReadTaskLogTool extends AbstractAgentTool {
    private final TaskAttemptRepository taskAttemptRepository;
    private final TaskEventRepository taskEventRepository;

    public DiagnosisReadTaskLogTool(TaskAttemptRepository taskAttemptRepository,
                                    TaskEventRepository taskEventRepository) {
        super("diagnosis.readTaskLog", "Read task logs for diagnosis.",
                AgentToolInputSchema.of(Set.of("taskId"), Map.of(
                        "taskId", "Task identifier whose attempt logs should be read.",
                        "attemptId", "Optional attempt identifier.",
                        "limit", "Maximum number of log lines or records to return.")),
                ToolRiskLevel.LOW,
                Set.of("AGENT_CHAT"));
        this.taskAttemptRepository = taskAttemptRepository;
        this.taskEventRepository = taskEventRepository;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        String taskId = text(input, "taskId");
        String attemptId = optionalText(input, "attemptId");
        int limit = intValue(input, "limit", 20);
        List<TaskAttempt> attempts = taskAttemptRepository.findByTaskId(taskId).stream()
                .filter(attempt -> attemptId == null || attemptId.isBlank() || attemptId.equals(attempt.attemptId()))
                .sorted(Comparator.comparing(TaskAttempt::startedAt))
                .limit(Math.max(limit, 1))
                .toList();
        return AgentToolResult.success(Map.of(
                "attempts", attempts.stream().map(this::attemptMap).toList(),
                "events", taskEventRepository.findByTaskId(taskId).stream()
                        .limit(Math.max(limit, 1))
                        .map(event -> Map.<String, Object>of(
                                "eventId", event.eventId(),
                                "eventType", event.eventType().name(),
                                "occurredAt", event.createdAt().toString(),
                                "payload", event.payload()))
                        .toList()));
    }

    private Map<String, Object> attemptMap(TaskAttempt attempt) {
        return Map.of(
                "attemptId", attempt.attemptId(),
                "attemptNo", attempt.attemptNo(),
                "result", attempt.result().name(),
                "rawLogObjectKey", defaultText(attempt.rawLogObjectKey()),
                "errorCode", defaultText(attempt.errorCode()),
                "errorMessage", defaultText(attempt.errorMessage()));
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
