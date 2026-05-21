package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.domain.AgentSession;
import com.zhongyan.uav.agent.domain.AgentSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaAgentSessionRepository implements AgentSessionRepository {
    private final AgentSessionJpaDataRepository dataRepository;

    public JpaAgentSessionRepository(AgentSessionJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public AgentSession save(AgentSession session) {
        return dataRepository.save(JpaAgentSessionEntity.fromDomain(session)).toDomain();
    }

    @Override
    public Optional<AgentSession> findById(String sessionId) {
        return dataRepository.findById(sessionId).map(JpaAgentSessionEntity::toDomain);
    }
}
