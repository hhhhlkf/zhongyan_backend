package com.zhongyan.uav.agent.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageJpaDataRepository extends JpaRepository<JpaAgentMessageEntity, String> {
    List<JpaAgentMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
