package com.zhongyan.uav.agent.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class AgentToolCall {
    private final String toolCallId;
    private final String sessionId;
    private final String messageId;
    private final String toolName;
    private final Map<String, Object> input;
    private final ToolRiskLevel riskLevel;
    private final boolean approvalRequired;
    private final String requestedBy;
    private AgentToolCallStatus status;
    private Map<String, Object> output;
    private String approvedBy;
    private Instant approvedAt;
    private String errorMessage;
    private final Instant createdAt;
    private Instant completedAt;

    public AgentToolCall(String toolCallId, String sessionId, String messageId, String toolName,
                         Map<String, Object> input, ToolRiskLevel riskLevel, boolean approvalRequired,
                         AgentToolCallStatus status, Instant createdAt) {
        this(toolCallId, sessionId, messageId, toolName, input, riskLevel, approvalRequired,
                defaultAgentRequester(sessionId), status, createdAt);
    }

    public AgentToolCall(String toolCallId, String sessionId, String messageId, String toolName,
                         Map<String, Object> input, ToolRiskLevel riskLevel, boolean approvalRequired,
                         String requestedBy, AgentToolCallStatus status, Instant createdAt) {
        this.toolCallId = requireText(toolCallId, "toolCallId");
        this.sessionId = requireText(sessionId, "sessionId");
        this.messageId = messageId;
        this.toolName = requireText(toolName, "toolName");
        this.input = input == null ? Map.of() : Map.copyOf(input);
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.approvalRequired = approvalRequired;
        this.requestedBy = requireAgentRequester(requestedBy);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.output = Map.of();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AgentToolCall create(String toolCallId, String sessionId, String messageId, String toolName,
                                       Map<String, Object> input, ToolRiskLevel riskLevel,
                                       boolean approvalRequired, Instant now) {
        return createForAgent(toolCallId, sessionId, messageId, toolName, input, riskLevel,
                approvalRequired, null, defaultAgentRequester(sessionId), now);
    }

    public static AgentToolCall createForAgent(String toolCallId, String sessionId, String messageId, String toolName,
                                               Map<String, Object> input, ToolRiskLevel riskLevel,
                                               boolean approvalRequired, String userId,
                                               String requestedBy, Instant now) {
        validateAgentRequester(requestedBy, sessionId, userId);
        AgentToolCallStatus status = approvalRequired || riskLevel.requiresApproval()
                ? AgentToolCallStatus.WAITING_APPROVAL
                : AgentToolCallStatus.RUNNING;
        return new AgentToolCall(toolCallId, sessionId, messageId, toolName, input, riskLevel,
                approvalRequired || riskLevel.requiresApproval(), requestedBy, status, now);
    }

    public static AgentToolCall createBlockedForAgent(String toolCallId, String sessionId, String messageId,
                                                      String toolName, Map<String, Object> input,
                                                      ToolRiskLevel riskLevel, boolean approvalRequired,
                                                      String userId, String requestedBy, String reason,
                                                      Instant now) {
        validateAgentRequester(requestedBy, sessionId, userId);
        AgentToolCall toolCall = new AgentToolCall(toolCallId, sessionId, messageId, toolName, input,
                riskLevel, approvalRequired || riskLevel.requiresApproval(), requestedBy,
                AgentToolCallStatus.BLOCKED, now);
        toolCall.errorMessage = reason;
        toolCall.completedAt = now;
        return toolCall;
    }

    public static AgentToolCall restore(String toolCallId, String sessionId, String messageId, String toolName,
                                        Map<String, Object> input, ToolRiskLevel riskLevel,
                                        boolean approvalRequired, AgentToolCallStatus status,
                                        Map<String, Object> output, String approvedBy, Instant approvedAt,
                                        String errorMessage, Instant createdAt, Instant completedAt) {
        AgentToolCall toolCall = new AgentToolCall(toolCallId, sessionId, messageId, toolName, input,
                riskLevel, approvalRequired, status, createdAt);
        toolCall.output = output == null ? Map.of() : Map.copyOf(output);
        toolCall.approvedBy = approvedBy;
        toolCall.approvedAt = approvedAt;
        toolCall.errorMessage = errorMessage;
        toolCall.completedAt = completedAt;
        return toolCall;
    }

    public static AgentToolCall restore(String toolCallId, String sessionId, String messageId, String toolName,
                                        Map<String, Object> input, ToolRiskLevel riskLevel,
                                        boolean approvalRequired, String requestedBy,
                                        AgentToolCallStatus status, Map<String, Object> output,
                                        String approvedBy, Instant approvedAt, String errorMessage,
                                        Instant createdAt, Instant completedAt) {
        AgentToolCall toolCall = new AgentToolCall(toolCallId, sessionId, messageId, toolName, input,
                riskLevel, approvalRequired, requestedBy, status, createdAt);
        toolCall.output = output == null ? Map.of() : Map.copyOf(output);
        toolCall.approvedBy = approvedBy;
        toolCall.approvedAt = approvedAt;
        toolCall.errorMessage = errorMessage;
        toolCall.completedAt = completedAt;
        return toolCall;
    }

    public void approve(String reviewer, Instant now) {
        if (status != AgentToolCallStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("Only waiting approval tool calls can be approved");
        }
        this.approvedBy = requireText(reviewer, "reviewer");
        this.approvedAt = Objects.requireNonNull(now, "now must not be null");
        this.status = AgentToolCallStatus.CREATED;
    }

    public void reject(String reason, Instant now) {
        if (status == AgentToolCallStatus.COMPLETED) {
            throw new IllegalStateException("Completed tool calls cannot be rejected");
        }
        this.errorMessage = reason;
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
        this.status = AgentToolCallStatus.REJECTED;
    }

    public void block(String reason, Instant now) {
        if (status == AgentToolCallStatus.COMPLETED) {
            throw new IllegalStateException("Completed tool calls cannot be blocked");
        }
        this.errorMessage = reason;
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
        this.status = AgentToolCallStatus.BLOCKED;
    }

    public void start() {
        if (status != AgentToolCallStatus.CREATED) {
            throw new IllegalStateException("Only created tool calls can start");
        }
        this.status = AgentToolCallStatus.RUNNING;
    }

    public void complete(Map<String, Object> output, Instant now) {
        if (status == AgentToolCallStatus.COMPLETED) {
            throw new IllegalStateException("Completed tool calls cannot be completed again");
        }
        if (status != AgentToolCallStatus.RUNNING) {
            throw new IllegalStateException("Only running tool calls can complete");
        }
        this.output = output == null ? Map.of() : Map.copyOf(output);
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
        this.status = AgentToolCallStatus.COMPLETED;
    }

    public void fail(String errorMessage, Instant now) {
        if (status == AgentToolCallStatus.COMPLETED) {
            throw new IllegalStateException("Completed tool calls cannot be failed");
        }
        if (status == AgentToolCallStatus.REJECTED) {
            throw new IllegalStateException("Rejected tool calls cannot be failed");
        }
        this.errorMessage = errorMessage;
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
        this.status = AgentToolCallStatus.FAILED;
    }

    public String toolCallId() {
        return toolCallId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String messageId() {
        return messageId;
    }

    public String toolName() {
        return toolName;
    }

    public Map<String, Object> input() {
        return input;
    }

    public ToolRiskLevel riskLevel() {
        return riskLevel;
    }

    public boolean approvalRequired() {
        return approvalRequired;
    }

    public String requestedBy() {
        return requestedBy;
    }

    public AgentToolCallStatus status() {
        return status;
    }

    public Map<String, Object> output() {
        return output;
    }

    public String approvedBy() {
        return approvedBy;
    }

    public Instant approvedAt() {
        return approvedAt;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String defaultAgentRequester(String sessionId) {
        return "agent:" + requireText(sessionId, "sessionId");
    }

    public static void validateAgentRequester(String requestedBy, String sessionId, String userId) {
        String requester = requireAgentRequester(requestedBy);
        String agentSession = defaultAgentRequester(sessionId);
        String agentUser = userId == null || userId.isBlank() ? null : "agent:" + userId;
        if (!requester.equals(agentSession) && !requester.equals(agentUser)) {
            throw new IllegalArgumentException("Agent write requestedBy must be agent:{sessionId} or agent:{userId}");
        }
    }

    private static String requireAgentRequester(String requestedBy) {
        String requester = requireText(requestedBy, "requestedBy");
        if (!requester.startsWith("agent:") || requester.length() == "agent:".length()) {
            throw new IllegalArgumentException("requestedBy must start with agent:");
        }
        return requester;
    }
}
