package com.zhongyan.uav.agent.infrastructure.mock;

import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.domain.AgentMessageRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAgentMessageRepository implements AgentMessageRepository {
    private final ConcurrentMap<String, AgentMessage> messages = new ConcurrentHashMap<>();

    @Override
    public AgentMessage save(AgentMessage message) {
        messages.put(message.messageId(), message);
        return message;
    }

    @Override
    public Optional<AgentMessage> findById(String messageId) {
        return Optional.ofNullable(messages.get(messageId));
    }

    @Override
    public List<AgentMessage> findBySessionId(String sessionId) {
        return messages.values().stream()
                .filter(message -> message.sessionId().equals(sessionId))
                .sorted(Comparator.comparing(AgentMessage::createdAt))
                .toList();
    }
}
