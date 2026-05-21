package com.zhongyan.uav.agent.infrastructure.mock;

import com.zhongyan.uav.agent.domain.AgentSession;
import com.zhongyan.uav.agent.domain.AgentSessionRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAgentSessionRepository implements AgentSessionRepository {
    private final ConcurrentMap<String, AgentSession> sessions = new ConcurrentHashMap<>();

    @Override
    public AgentSession save(AgentSession session) {
        sessions.put(session.sessionId(), session);
        return session;
    }

    @Override
    public Optional<AgentSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}
