package com.zhongyan.uav.agent.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentSessionJpaDataRepository extends JpaRepository<JpaAgentSessionEntity, String> {
}
