package com.zhongyan.uav.agent.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentKnowledgeDocumentJpaDataRepository
        extends JpaRepository<JpaAgentKnowledgeDocumentEntity, String> {
    List<JpaAgentKnowledgeDocumentEntity> findBySourceTypeAndSourceId(String sourceType, String sourceId);
}
