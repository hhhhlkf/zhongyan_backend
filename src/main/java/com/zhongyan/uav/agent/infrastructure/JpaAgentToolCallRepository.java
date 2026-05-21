package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.domain.AgentToolCallRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaAgentToolCallRepository implements AgentToolCallRepository {
    private final AgentToolCallJpaDataRepository dataRepository;

    public JpaAgentToolCallRepository(AgentToolCallJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public AgentToolCall save(AgentToolCall toolCall) {
        return dataRepository.save(JpaAgentToolCallEntity.fromDomain(toolCall)).toDomain();
    }

    @Override
    public Optional<AgentToolCall> findById(String toolCallId) {
        return dataRepository.findById(toolCallId).map(JpaAgentToolCallEntity::toDomain);
    }

    @Override
    public List<AgentToolCall> findBySessionId(String sessionId) {
        return dataRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(JpaAgentToolCallEntity::toDomain)
                .collect(Collectors.toList());
    }
}
