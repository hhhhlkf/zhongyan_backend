package com.zhongyan.uav.agent.domain;

import java.util.Optional;

public interface AgentSessionRepository {
    AgentSession save(AgentSession session);

    Optional<AgentSession> findById(String sessionId);
}
