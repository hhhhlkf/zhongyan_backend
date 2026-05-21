package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.domain.AgentToolCall;

import java.util.List;

/**
 * 一次 Agent 对话编排的应用层结果，包含用户消息、助手回复和本轮工具调用轨迹。
 */
public record AgentInteractionResult(
        AgentMessage userMessage,
        AgentMessage assistantMessage,
        List<AgentToolCall> toolCalls) {
    public AgentInteractionResult {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
