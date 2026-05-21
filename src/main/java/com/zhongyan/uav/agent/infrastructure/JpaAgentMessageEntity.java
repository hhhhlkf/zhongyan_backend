package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.domain.AgentMessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "agent_message")
public class JpaAgentMessageEntity {
    @Id
    @Column(name = "message_id", length = 120)
    private String messageId;

    @Column(name = "session_id", nullable = false, length = 120)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private AgentMessageRole role;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    protected JpaAgentMessageEntity() {
    }

    public static JpaAgentMessageEntity fromDomain(AgentMessage message) {
        JpaAgentMessageEntity entity = new JpaAgentMessageEntity();
        entity.messageId = message.messageId();
        entity.sessionId = message.sessionId();
        entity.role = message.role();
        entity.content = message.content();
        entity.tokenCount = null;
        entity.createdAt = message.createdAt();
        entity.metadata = message.metadata();
        return entity;
    }

    public AgentMessage toDomain() {
        return new AgentMessage(messageId, sessionId, role, content, metadata, createdAt);
    }
}
