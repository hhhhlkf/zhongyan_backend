package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.task.application.CreateTaskCommandInput;
import com.zhongyan.uav.task.application.TaskCommandApplicationService;
import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandType;

import java.util.Map;
import java.util.Set;

/**
 * Agent tool that creates a TaskCommand instead of executing high-risk work directly.
 * <p>
 * The tool is intentionally high risk and approval-gated. After approval it delegates to
 * {@link TaskCommandApplicationService}, so device actions still flow through TaskCommand audit,
 * approval, and scheduling rules.
 */
public class TaskCommandCreateTool extends AbstractAgentTool {
    private final TaskCommandApplicationService taskCommandApplicationService;

    public TaskCommandCreateTool() {
        this(null);
    }

    public TaskCommandCreateTool(TaskCommandApplicationService taskCommandApplicationService) {
        super("taskCommand.create", "Create a TaskCommand for high-risk device or layer actions.",
                AgentToolInputSchema.of(Set.of("taskId", "commandType", "reason"), Map.of(
                        "taskId", "Task identifier that owns the command.",
                        "commandType", "Command type to create.",
                        "payload", "Structured command payload.",
                        "idempotencyKey", "Optional caller-supplied idempotency key.",
                        "reason", "Human-readable reason for audit and approval.",
                        "deviceId", "Optional device override.",
                        "riskLevel", "Optional explicit risk level.")),
                ToolRiskLevel.HIGH, Set.of("AGENT_TOOL_APPROVE"));
        this.taskCommandApplicationService = taskCommandApplicationService;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        if (taskCommandApplicationService == null) {
            return AgentToolResult.blocked("TaskCommand application service is not configured");
        }

        TaskCommandType commandType = TaskCommandType.valueOf(text(input, "commandType").toUpperCase());
        RiskLevel riskLevel = optionalText(input, "riskLevel") == null
                ? commandType.defaultRiskLevel()
                : RiskLevel.valueOf(optionalText(input, "riskLevel").toUpperCase());
        String idempotencyKey = defaultText(optionalText(input, "idempotencyKey"),
                "agent-tool-" + context.attributes().getOrDefault("toolCallId", context.sessionId()));

        // Agent 写操作只创建 TaskCommand，真实设备动作由后续 TaskCommand 审批和调度链路处理。
        TaskCommand command = taskCommandApplicationService.createCommand(text(input, "taskId"),
                new CreateTaskCommandInput(commandType, mapValue(input, "payload"), idempotencyKey,
                        context.requestedBy(), riskLevel, optionalText(input, "deviceId"),
                        text(input, "reason")));

        return AgentToolResult.success(Map.of(
                "commandId", command.commandId(),
                "taskId", command.taskId(),
                "commandType", command.commandType().name(),
                "commandStatus", command.status().name(),
                "requiresApproval", command.requiresApproval()));
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
