package com.zhongyan.uav.agent.infrastructure.mock;

import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.domain.AgentToolCallRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAgentToolCallRepository implements AgentToolCallRepository {
    private final ConcurrentMap<String, AgentToolCall> toolCalls = new ConcurrentHashMap<>();

    @Override
    public AgentToolCall save(AgentToolCall toolCall) {
        toolCalls.put(toolCall.toolCallId(), toolCall);
        return toolCall;
    }

    @Override
    public Optional<AgentToolCall> findById(String toolCallId) {
        return Optional.ofNullable(toolCalls.get(toolCallId));
    }

    @Override
    public List<AgentToolCall> findBySessionId(String sessionId) {
        return toolCalls.values().stream()
                .filter(toolCall -> toolCall.sessionId().equals(sessionId))
                .sorted(Comparator.comparing(AgentToolCall::createdAt))
                .toList();
    }
}
