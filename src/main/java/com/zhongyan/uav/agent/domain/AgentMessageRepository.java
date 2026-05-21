package com.zhongyan.uav.agent.domain;

import java.util.List;
import java.util.Optional;

public interface AgentMessageRepository {
    AgentMessage save(AgentMessage message);

    Optional<AgentMessage> findById(String messageId);

    List<AgentMessage> findBySessionId(String sessionId);
}
