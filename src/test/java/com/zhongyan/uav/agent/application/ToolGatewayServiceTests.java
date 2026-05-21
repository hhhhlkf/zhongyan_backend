package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.domain.AgentToolCallStatus;
import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentToolCallRepository;
import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolGatewayServiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-19T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void lowRiskToolExecutesImmediatelyAndWritesAuditEvents() {
        InMemoryAgentToolCallRepository repository = new InMemoryAgentToolCallRepository();
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        CountingTool tool = new CountingTool("task.query", ToolRiskLevel.LOW, false, Set.of("AGENT_CHAT"));
        ToolGatewayService service = new ToolGatewayService(List.of(tool), repository, publisher, CLOCK);

        AgentToolCall toolCall = service.invoke(new InvokeAgentToolCommand("session-1", "message-1",
                "user-1", "task.query", Map.of("taskId", "task-1"), Set.of("AGENT_CHAT")));

        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.COMPLETED);
        assertThat(toolCall.output()).containsEntry("executedBy", "agent:session-1");
        assertThat(tool.executeCount()).isEqualTo(1);
        assertThat(repository.findById(toolCall.toolCallId())).contains(toolCall);
        assertThat(publisher.eventTypes()).containsExactly("TOOL_SELECTED", "TOOL_CALL_STARTED",
                "TOOL_CALL_COMPLETED");
    }

    @Test
    void highRiskToolWaitsForApprovalThenExecutesThroughGateway() {
        InMemoryAgentToolCallRepository repository = new InMemoryAgentToolCallRepository();
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        CountingTool tool = new CountingTool("taskCommand.create", ToolRiskLevel.HIGH, true,
                Set.of("AGENT_TOOL_APPROVE"));
        ToolGatewayService service = new ToolGatewayService(List.of(tool), repository, publisher, CLOCK);

        AgentToolCall waiting = service.invoke(new InvokeAgentToolCommand("session-1", "message-1",
                "user-1", "taskCommand.create", Map.of("taskId", "task-1"),
                Set.of("AGENT_TOOL_APPROVE")));

        assertThat(waiting.status()).isEqualTo(AgentToolCallStatus.WAITING_APPROVAL);
        assertThat(tool.executeCount()).isZero();

        AgentToolCall approved = service.approve(waiting.toolCallId(), "reviewer-1");

        assertThat(approved.status()).isEqualTo(AgentToolCallStatus.COMPLETED);
        assertThat(approved.approvedBy()).isEqualTo("reviewer-1");
        assertThat(tool.executeCount()).isEqualTo(1);
        assertThat(publisher.eventTypes()).contains("APPROVAL_REQUESTED", "APPROVAL_CONFIRMED",
                "TOOL_CALL_STARTED", "TOOL_CALL_COMPLETED");
    }

    @Test
    void blockedToolCallIsAuditedWhenPermissionIsMissing() {
        InMemoryAgentToolCallRepository repository = new InMemoryAgentToolCallRepository();
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        CountingTool tool = new CountingTool("task.query", ToolRiskLevel.LOW, false, Set.of("AGENT_CHAT"));
        ToolGatewayService service = new ToolGatewayService(List.of(tool), repository, publisher, CLOCK);

        AgentToolCall blocked = service.invoke(new InvokeAgentToolCommand("session-1", "message-1",
                "user-1", "task.query", Map.of("taskId", "task-1"), Set.of()));

        assertThat(blocked.status()).isEqualTo(AgentToolCallStatus.BLOCKED);
        assertThat(blocked.errorMessage()).contains("Missing Agent tool permissions");
        assertThat(tool.executeCount()).isZero();
        assertThat(repository.findById(blocked.toolCallId())).contains(blocked);
        assertThat(publisher.eventTypes()).containsExactly("TOOL_SELECTED", "TOOL_CALL_BLOCKED");
    }

    @Test
    void rejectedToolCallDoesNotExecute() {
        InMemoryAgentToolCallRepository repository = new InMemoryAgentToolCallRepository();
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        CountingTool tool = new CountingTool("asset.delete", ToolRiskLevel.CRITICAL, true,
                Set.of("AGENT_TOOL_APPROVE"));
        ToolGatewayService service = new ToolGatewayService(List.of(tool), repository, publisher, CLOCK);

        AgentToolCall waiting = service.invoke(new InvokeAgentToolCommand("session-1", "message-1",
                "user-1", "asset.delete", Map.of("taskId", "task-1"),
                Set.of("AGENT_TOOL_APPROVE")));
        AgentToolCall rejected = service.reject(waiting.toolCallId(), "risk too high");

        assertThat(rejected.status()).isEqualTo(AgentToolCallStatus.REJECTED);
        assertThat(tool.executeCount()).isZero();
        assertThat(publisher.eventTypes()).contains("APPROVAL_REQUESTED", "TOOL_CALL_BLOCKED");
    }

    private static final class CountingTool implements AgentTool {
        private final String name;
        private final ToolRiskLevel riskLevel;
        private final boolean approvalRequired;
        private final Set<String> requiredPermissions;
        private int executeCount;

        private CountingTool(String name, ToolRiskLevel riskLevel, boolean approvalRequired,
                             Set<String> requiredPermissions) {
            this.name = name;
            this.riskLevel = riskLevel;
            this.approvalRequired = approvalRequired;
            this.requiredPermissions = requiredPermissions;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "test tool";
        }

        @Override
        public AgentToolInputSchema inputSchema() {
            return AgentToolInputSchema.of(Set.of("taskId"), Map.of("taskId", "Task identifier."));
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return riskLevel;
        }

        @Override
        public boolean approvalRequired() {
            return approvalRequired;
        }

        @Override
        public Set<String> requiredPermissions() {
            return requiredPermissions;
        }

        @Override
        public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
            executeCount++;
            return AgentToolResult.success(Map.of("executedBy", context.requestedBy()));
        }

        private int executeCount() {
            return executeCount;
        }
    }

    private static final class CollectingAgentEventPublisher implements AgentEventPublisher {
        private final List<AgentEvent> events = new ArrayList<>();

        @Override
        public void publish(AgentEvent event) {
            events.add(event);
        }

        private List<String> eventTypes() {
            return events.stream().map(AgentEvent::eventType).toList();
        }
    }
}
