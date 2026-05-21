package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.domain.AgentMessageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaAgentMessageRepository implements AgentMessageRepository {
    private final AgentMessageJpaDataRepository dataRepository;

    public JpaAgentMessageRepository(AgentMessageJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public AgentMessage save(AgentMessage message) {
        return dataRepository.save(JpaAgentMessageEntity.fromDomain(message)).toDomain();
    }

    @Override
    public Optional<AgentMessage> findById(String messageId) {
        return dataRepository.findById(messageId).map(JpaAgentMessageEntity::toDomain);
    }

    @Override
    public List<AgentMessage> findBySessionId(String sessionId) {
        return dataRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(JpaAgentMessageEntity::toDomain)
                .collect(Collectors.toList());
    }
}
