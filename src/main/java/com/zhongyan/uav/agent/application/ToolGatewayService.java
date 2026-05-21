package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.domain.AgentToolCallRepository;
import com.zhongyan.uav.agent.domain.AgentToolCallStatus;
import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.agent.port.AgentRuntimeGuard;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent 工具网关应用服务。
 * <p>
 * 这是 Agent 调用系统能力的唯一入口，统一处理工具查找、权限校验、输入校验、
 * 风险审批、审计记录、工具执行和事件发布。
 */
public class ToolGatewayService {
    private final Map<String, AgentTool> tools;
    private final AgentToolCallRepository toolCallRepository;
    private final AgentEventPublisher eventPublisher;
    private final AgentRuntimeGuard runtimeGuard;
    private final Clock clock;

    public ToolGatewayService(List<AgentTool> tools,
                              AgentToolCallRepository toolCallRepository,
                              AgentEventPublisher eventPublisher,
                              Clock clock) {
        this(tools, toolCallRepository, eventPublisher, AgentRuntimeGuard.noop(), clock);
    }

    public ToolGatewayService(List<AgentTool> tools,
                              AgentToolCallRepository toolCallRepository,
                              AgentEventPublisher eventPublisher,
                              AgentRuntimeGuard runtimeGuard,
                              Clock clock) {
        this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(AgentTool::name, Function.identity()));
        this.toolCallRepository = toolCallRepository;
        this.eventPublisher = eventPublisher;
        this.runtimeGuard = runtimeGuard;
        this.clock = clock;
    }

    public AgentToolCall invoke(InvokeAgentToolCommand command) {
        AgentTool tool = findTool(command.toolName());
        String requestedBy = "agent:" + command.sessionId();
        publish(command.sessionId(), "TOOL_SELECTED", Map.of(
                "toolName", tool.name(),
                "riskLevel", tool.riskLevel().name(),
                "approvalRequired", tool.approvalRequired()));

        // 权限或输入不满足时仍写入 BLOCKED 审计记录，保证 Agent 行为可回放。
        try {
            runtimeGuard.assertToolCallAllowed(command.sessionId(), command.userId(), tool.name());
            validatePermissions(command.permissions(), tool.requiredPermissions());
            tool.inputSchema().validate(command.input());
        } catch (RuntimeException ex) {
            return block(command, tool, requestedBy, ex.getMessage());
        }

        String toolCallId = "agent-tool-call-" + UUID.randomUUID();
        AgentToolCall duplicate = runtimeGuard.reserveIdempotencyKey(command.sessionId(), tool.name(),
                        command.input(), toolCallId)
                .flatMap(toolCallRepository::findById)
                .orElse(null);
        if (duplicate != null) {
            publish(command.sessionId(), "TOOL_CALL_DEDUPLICATED", Map.of(
                    "toolCallId", duplicate.toolCallId(),
                    "toolName", duplicate.toolName()));
            return duplicate;
        }

        AgentToolCall toolCall = AgentToolCall.createForAgent(toolCallId,
                command.sessionId(), command.messageId(), tool.name(), command.input(), tool.riskLevel(),
                tool.approvalRequired(), command.userId(), requestedBy, clock.instant());
        toolCallRepository.save(toolCall);

        if (toolCall.status() == AgentToolCallStatus.WAITING_APPROVAL) {
            publish(toolCall.sessionId(), "APPROVAL_REQUESTED", Map.of("toolCallId", toolCall.toolCallId()));
            return toolCall;
        }

        // 低风险或无需审批的工具在同一条审计链路内自动执行。
        return execute(toolCall, tool, command.userId(), command.permissions());
    }

    public AgentToolCall approve(String toolCallId, String reviewer) {
        AgentToolCall toolCall = requireToolCall(toolCallId);
        AgentTool tool = findTool(toolCall.toolName());
        toolCall.approve(reviewer, clock.instant());
        toolCallRepository.save(toolCall);
        publish(toolCall.sessionId(), "APPROVAL_CONFIRMED", Map.of(
                "toolCallId", toolCall.toolCallId(),
                "toolName", toolCall.toolName(),
                "reviewer", reviewer));
        // 审批通过只改变策略状态，真实执行仍回到统一 execute 流程。
        return execute(toolCall, tool, reviewer, tool.requiredPermissions());
    }

    public AgentToolCall reject(String toolCallId, String reason) {
        AgentToolCall toolCall = requireToolCall(toolCallId);
        toolCall.reject(reason, clock.instant());
        toolCallRepository.save(toolCall);
        publish(toolCall.sessionId(), "TOOL_CALL_BLOCKED", Map.of(
                "toolCallId", toolCall.toolCallId(),
                "toolName", toolCall.toolName(),
                "reason", defaultText(reason, "rejected")));
        return toolCall;
    }

    public AgentToolCall getToolCall(String toolCallId) {
        return requireToolCall(toolCallId);
    }

    public List<String> listToolNames() {
        return tools.keySet().stream().sorted().toList();
    }

    public List<AgentToolCall> listToolCalls(String sessionId) {
        return toolCallRepository.findBySessionId(sessionId);
    }

    private AgentToolCall execute(AgentToolCall toolCall, AgentTool tool, String userId, Set<String> permissions) {
        try {
            if (toolCall.status() == AgentToolCallStatus.CREATED) {
                toolCall.start();
                toolCallRepository.save(toolCall);
            }
            if (toolCall.status() != AgentToolCallStatus.RUNNING) {
                throw new IllegalStateException("Only running tool calls can execute");
            }
            publish(toolCall.sessionId(), "TOOL_CALL_STARTED", Map.of(
                    "toolCallId", toolCall.toolCallId(),
                    "toolName", toolCall.toolName()));
            // 工具只接收应用层上下文，不能在 Gateway 外绕过审计直接操作基础设施。
            AgentToolContext context = new AgentToolContext(toolCall.sessionId(), userId,
                    toolCall.requestedBy(), permissions, Map.of("toolCallId", toolCall.toolCallId()));
            AgentToolResult result = tool.execute(context, toolCall.input());
            if (!result.success()) {
                toolCall.fail(result.message(), clock.instant());
                publish(toolCall.sessionId(), "TOOL_CALL_FAILED", Map.of(
                        "toolCallId", toolCall.toolCallId(),
                        "toolName", toolCall.toolName(),
                        "errorMessage", defaultText(result.message(), "tool returned failure")));
            } else {
                toolCall.complete(result.output(), clock.instant());
                publish(toolCall.sessionId(), "TOOL_CALL_COMPLETED", Map.of(
                        "toolCallId", toolCall.toolCallId(),
                        "toolName", toolCall.toolName()));
            }
            return toolCallRepository.save(toolCall);
        } catch (RuntimeException ex) {
            toolCall.fail(ex.getMessage(), clock.instant());
            toolCallRepository.save(toolCall);
            publish(toolCall.sessionId(), "TOOL_CALL_FAILED", Map.of(
                    "toolCallId", toolCall.toolCallId(),
                    "toolName", toolCall.toolName(),
                    "errorMessage", defaultText(ex.getMessage(), ex.getClass().getSimpleName())));
            throw ex;
        }
    }

    private AgentToolCall block(InvokeAgentToolCommand command, AgentTool tool, String requestedBy, String reason) {
        Instant now = clock.instant();
        AgentToolCall toolCall = AgentToolCall.createBlockedForAgent("agent-tool-call-" + UUID.randomUUID(),
                command.sessionId(), command.messageId(), tool.name(), command.input(), tool.riskLevel(),
                tool.approvalRequired(), command.userId(), requestedBy, defaultText(reason, "blocked"), now);
        toolCallRepository.save(toolCall);
        publish(toolCall.sessionId(), "TOOL_CALL_BLOCKED", Map.of(
                "toolCallId", toolCall.toolCallId(),
                "toolName", toolCall.toolName(),
                "reason", defaultText(reason, "blocked")));
        return toolCall;
    }

    private AgentTool findTool(String toolName) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Agent tool not found: " + toolName);
        }
        return tool;
    }

    private AgentToolCall requireToolCall(String toolCallId) {
        return toolCallRepository.findById(toolCallId)
                .orElseThrow(() -> new IllegalArgumentException("Agent tool call not found: " + toolCallId));
    }

    private void validatePermissions(Set<String> actual, Set<String> required) {
        if (!actual.containsAll(required)) {
            throw new SecurityException("Missing Agent tool permissions: " + required);
        }
    }

    private void publish(String sessionId, String eventType, Map<String, Object> payload) {
        eventPublisher.publish(new AgentEvent("agent-event-" + UUID.randomUUID(), sessionId, eventType,
                payload, clock.instant()));
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
