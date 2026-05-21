package com.zhongyan.uav.agent.domain;

import java.util.List;
import java.util.Optional;

public interface AgentToolCallRepository {
    AgentToolCall save(AgentToolCall toolCall);

    Optional<AgentToolCall> findById(String toolCallId);

    List<AgentToolCall> findBySessionId(String sessionId);
}
