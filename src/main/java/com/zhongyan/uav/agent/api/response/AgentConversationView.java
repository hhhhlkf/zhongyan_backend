package com.zhongyan.uav.agent.api.response;

import com.zhongyan.uav.agent.application.AgentInteractionResult;

import java.util.List;

/**
 * 一次用户消息触发的完整 Agent 编排结果。
 */
public record AgentConversationView(
        AgentMessageView userMessage,
        AgentMessageView assistantMessage,
        List<AgentToolCallView> toolCalls) {
    public AgentConversationView {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AgentConversationView fromResult(AgentInteractionResult result) {
        return new AgentConversationView(AgentMessageView.fromDomain(result.userMessage()),
                AgentMessageView.fromDomain(result.assistantMessage()),
                result.toolCalls().stream().map(AgentToolCallView::fromDomain).toList());
    }
}
