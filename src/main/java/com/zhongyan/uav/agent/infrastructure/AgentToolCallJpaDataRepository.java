package com.zhongyan.uav.agent.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentToolCallJpaDataRepository extends JpaRepository<JpaAgentToolCallEntity, String> {
    List<JpaAgentToolCallEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
