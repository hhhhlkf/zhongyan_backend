package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.domain.AgentToolCallStatus;
import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "agent_tool_call")
public class JpaAgentToolCallEntity {
    @Id
    @Column(name = "tool_call_id", length = 120)
    private String toolCallId;

    @Column(name = "session_id", length = 120)
    private String sessionId;

    @Column(name = "message_id", length = 120)
    private String messageId;

    @Column(name = "tool_name", nullable = false, length = 160)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private ToolRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AgentToolCallStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "arguments", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> arguments = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> result = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> input = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> output = Map.of();

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    protected JpaAgentToolCallEntity() {
    }

    public static JpaAgentToolCallEntity fromDomain(AgentToolCall toolCall) {
        JpaAgentToolCallEntity entity = new JpaAgentToolCallEntity();
        entity.toolCallId = toolCall.toolCallId();
        entity.sessionId = toolCall.sessionId();
        entity.messageId = toolCall.messageId();
        entity.toolName = toolCall.toolName();
        entity.riskLevel = toolCall.riskLevel();
        entity.status = toolCall.status();
        entity.arguments = toolCall.input();
        entity.result = toolCall.output();
        entity.input = toolCall.input();
        entity.output = toolCall.output();
        entity.approvalRequired = toolCall.approvalRequired();
        entity.requestedBy = toolCall.requestedBy();
        entity.reviewedBy = toolCall.approvedBy();
        entity.reviewedAt = toolCall.approvedAt();
        entity.approvedBy = toolCall.approvedBy();
        entity.approvedAt = toolCall.approvedAt();
        entity.createdAt = toolCall.createdAt();
        entity.completedAt = toolCall.completedAt();
        entity.errorMessage = toolCall.errorMessage();
        return entity;
    }

    public AgentToolCall toDomain() {
        Map<String, Object> effectiveInput = input == null || input.isEmpty() ? arguments : input;
        Map<String, Object> effectiveOutput = output == null || output.isEmpty() ? result : output;
        String reviewer = approvedBy == null ? reviewedBy : approvedBy;
        Instant reviewedAtValue = approvedAt == null ? reviewedAt : approvedAt;
        return AgentToolCall.restore(toolCallId, sessionId, messageId, toolName, effectiveInput, riskLevel,
                approvalRequired, requestedBy, status, effectiveOutput, reviewer, reviewedAtValue,
                errorMessage, createdAt, completedAt);
    }
}
