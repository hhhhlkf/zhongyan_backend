package com.zhongyan.uav.agent.port;

import com.zhongyan.uav.agent.domain.AgentMessage;

import java.util.List;
import java.util.Map;

public record AgentChatRequest(
        String sessionId,
        String userId,
        List<AgentMessage> messages,
        Map<String, Object> context) {
    public AgentChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
