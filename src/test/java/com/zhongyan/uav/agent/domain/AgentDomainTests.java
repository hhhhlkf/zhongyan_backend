package com.zhongyan.uav.agent.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDomainTests {
    private static final Instant NOW = Instant.parse("2026-05-19T08:00:00Z");

    @Test
    void createsSessionWithRequiredLifecycleFields() {
        AgentSession session = AgentSession.create("session-1", "mission-1", "task-1",
                "user-1", "Mission analysis", NOW);

        assertThat(session.sessionId()).isEqualTo("session-1");
        assertThat(session.missionId()).isEqualTo("mission-1");
        assertThat(session.taskId()).isEqualTo("task-1");
        assertThat(session.userId()).isEqualTo("user-1");
        assertThat(session.title()).isEqualTo("Mission analysis");
        assertThat(session.status()).isEqualTo("ACTIVE");
        assertThat(session.createdAt()).isEqualTo(NOW);
        assertThat(session.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void createsMessageWithImmutableMetadata() {
        AgentMessage message = new AgentMessage("message-1", "session-1", AgentMessageRole.USER,
                "Summarize the mission", Map.of("source", "test"), NOW);

        assertThat(message.messageId()).isEqualTo("message-1");
        assertThat(message.sessionId()).isEqualTo("session-1");
        assertThat(message.role()).isEqualTo(AgentMessageRole.USER);
        assertThat(message.content()).isEqualTo("Summarize the mission");
        assertThat(message.metadata()).containsEntry("source", "test");
        assertThatThrownBy(() -> message.metadata().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void lowRiskToolCallEntersRunningAndCannotRewriteOutputAfterCompletion() {
        AgentToolCall toolCall = AgentToolCall.create("call-low", "session-1", "message-1",
                "task.query", Map.of("taskId", "task-1"), ToolRiskLevel.LOW, false, NOW);

        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.RUNNING);
        assertThat(toolCall.requestedBy()).isEqualTo("agent:session-1");

        toolCall.complete(Map.of("status", "DONE"), NOW.plusSeconds(1));

        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.COMPLETED);
        assertThat(toolCall.output()).containsEntry("status", "DONE");
        assertThatThrownBy(() -> toolCall.complete(Map.of("status", "OVERWRITTEN"), NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Completed");
        assertThatThrownBy(() -> toolCall.fail("late failure", NOW.plusSeconds(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Completed");
    }

    @Test
    void highRiskToolCallWaitsForApprovalBeforeRunning() {
        AgentToolCall toolCall = AgentToolCall.create("call-high", "session-1", "message-1",
                "command.createStartCapture", Map.of("taskId", "task-1"), ToolRiskLevel.HIGH,
                false, NOW);

        assertThat(toolCall.approvalRequired()).isTrue();
        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.WAITING_APPROVAL);
        assertThatThrownBy(toolCall::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only created");

        toolCall.approve("reviewer-1", NOW.plusSeconds(1));
        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.CREATED);
        assertThat(toolCall.approvedBy()).isEqualTo("reviewer-1");

        toolCall.start();
        toolCall.complete(Map.of("commandId", "command-1"), NOW.plusSeconds(2));

        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.COMPLETED);
    }

    @Test
    void rejectedToolCallCannotExecuteAgain() {
        AgentToolCall toolCall = AgentToolCall.create("call-rejected", "session-1", "message-1",
                "asset.delete", Map.of("assetId", "asset-1"), ToolRiskLevel.CRITICAL, false, NOW);

        toolCall.reject("not allowed", NOW.plusSeconds(1));

        assertThat(toolCall.status()).isEqualTo(AgentToolCallStatus.REJECTED);
        assertThatThrownBy(toolCall::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only created");
        assertThatThrownBy(() -> toolCall.fail("should not rewrite", NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Rejected");
    }

    @Test
    void validatesAgentWriteRequester() {
        AgentToolCall sessionRequester = AgentToolCall.createForAgent("call-agent-session",
                "session-1", "message-1", "task.create", Map.of(), ToolRiskLevel.MEDIUM,
                false, "user-1", "agent:session-1", NOW);
        AgentToolCall userRequester = AgentToolCall.createForAgent("call-agent-user",
                "session-1", "message-1", "task.create", Map.of(), ToolRiskLevel.MEDIUM,
                false, "user-1", "agent:user-1", NOW);

        assertThat(sessionRequester.requestedBy()).isEqualTo("agent:session-1");
        assertThat(userRequester.requestedBy()).isEqualTo("agent:user-1");
        assertThatThrownBy(() -> AgentToolCall.createForAgent("call-invalid-prefix",
                "session-1", "message-1", "task.create", Map.of(), ToolRiskLevel.MEDIUM,
                false, "user-1", "operator-1", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agent:");
        assertThatThrownBy(() -> AgentToolCall.createForAgent("call-invalid-agent",
                "session-1", "message-1", "task.create", Map.of(), ToolRiskLevel.MEDIUM,
                false, "user-1", "agent:other", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agent:{sessionId}");
    }
}
